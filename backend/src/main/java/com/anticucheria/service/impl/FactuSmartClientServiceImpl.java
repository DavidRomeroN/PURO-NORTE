package com.anticucheria.service.impl;

import com.anticucheria.config.FactuSmartConfig;
import com.anticucheria.exception.FactuSmartException;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.service.FactuSmartClientService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.FactuSmartPayloadBuilder;
import com.anticucheria.service.factusmart.FactuSmartRespuesta;
import com.anticucheria.service.factusmart.FactuSmartResponseMapper;
import com.anticucheria.service.factusmart.InterruptorDeEmision;
import com.anticucheria.service.factusmart.TipoArchivo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Integracion HTTP con FactuSmart para boletas de venta.
 *
 * Dos cosas que gobiernan todo el diseno de esta clase:
 *
 * 1. Cada emision cuesta un credito, tambien en sandbox. En el perfil dev y sin API key
 *    no se sale a la red: se simula, para no quemar los 100 creditos que hay. Fuera de
 *    dev eso no es posible, porque {@code ComprobacionDeFactuSmart} no deja arrancar sin
 *    API key. De ahi el invariante que usan todos los metodos de aca: si
 *    {@code debeSimular()} es falso, hay API key.
 * 2. Un HTTP 200 no significa "aceptado por SUNAT". Nada de aca marca una boleta como
 *    aceptada; eso lo decide el mapper leyendo el cuerpo de la respuesta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactuSmartClientServiceImpl implements FactuSmartClientService {

    private static final String RUTA_DOCUMENTOS = "/documents";
    private static final int INTENTOS_SI_HAY_EMISION_EN_CURSO = 2;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 3000;

    private final FactuSmartConfig config;
    private final RestClient factuSmartRestClient;
    private final FactuSmartPayloadBuilder payloadBuilder;
    private final FactuSmartResponseMapper responseMapper;
    private final InterruptorDeEmision interruptor;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void comprobarConfiguracionAlArrancar() {
        if (config.debeSimular()) {
            // El aviso fuerte ya lo dio ComprobacionDeFactuSmart al arrancar.
            return;
        }
        if (ping()) {
            log.info("FactuSmart responde correctamente en {}", config.getUrlBase());
        } else {
            log.warn("FactuSmart no respondio al ping en {}. La app arranca igual, pero las "
                    + "emisiones fallaran hasta que se corrija la URL o la API key.", config.getUrlBase());
        }
    }

    @Override
    public FactuSmartRespuesta emitir(Boleta boleta, List<PedidoItem> items) {
        if (interruptor.estaBloqueado()) {
            throw new FactuSmartException("Emision suspendida. " + interruptor.motivo(), false);
        }
        if (config.debeSimular()) {
            return simular(boleta);
        }

        Map<String, Object> payload = payloadBuilder.construir(boleta, items);

        // La clave se deriva del id ya persistido, no de un UUID del momento: si se corta
        // el internet a mitad del envio y la app reintenta, la API devuelve el mismo
        // comprobante en vez de emitir un duplicado fiscal, y no cobra otro credito.
        String idempotencyKey = "boleta-" + boleta.getId();

        for (int intento = 1; ; intento++) {
            ResponseEntity<String> respuesta = factuSmartRestClient.post()
                    .uri(RUTA_DOCUMENTOS)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            if (respuesta.getStatusCode().value() == 409 && intento <= INTENTOS_SI_HAY_EMISION_EN_CURSO) {
                // La primera peticion todavia se esta procesando. No es un error.
                log.info("Boleta {}: emision en curso con la misma clave, reintentando", boleta.getId());
                esperar();
                continue;
            }

            return interpretar(respuesta, "emitir la boleta " + boleta.getId());
        }
    }

    @Override
    public FactuSmartRespuesta reenviar(Boleta boleta) {
        if (config.debeSimular()) {
            return simular(boleta);
        }
        exigirExternalId(boleta);

        ResponseEntity<String> respuesta = factuSmartRestClient.post()
                .uri(RUTA_DOCUMENTOS + "/{externalId}/reenviar", boleta.getExternalId())
                .body(Map.of("ruc", config.getRuc()))
                .retrieve()
                .toEntity(String.class);

        return interpretar(respuesta, "reenviar la boleta " + boleta.getId());
    }

    @Override
    public FactuSmartRespuesta consultarEnSunat(Boleta boleta) {
        if (config.debeSimular()) {
            return simular(boleta);
        }
        exigirExternalId(boleta);

        ResponseEntity<String> respuesta = factuSmartRestClient.post()
                .uri(RUTA_DOCUMENTOS + "/{externalId}/consultar-sunat", boleta.getExternalId())
                .body(Map.of("ruc", config.getRuc()))
                .retrieve()
                .toEntity(String.class);

        return interpretar(respuesta, "consultar la boleta " + boleta.getId());
    }

    @Override
    public ArchivoComprobante descargar(Boleta boleta, TipoArchivo tipo) {
        if (config.debeSimular()) {
            throw new FactuSmartException("FactuSmart no esta configurado: no hay archivos que descargar", false);
        }
        exigirExternalId(boleta);

        ResponseEntity<byte[]> respuesta = factuSmartRestClient.get()
                .uri(uri -> uri.path(RUTA_DOCUMENTOS + "/{externalId}/" + tipo.ruta())
                        .queryParam("ruc", config.getRuc())
                        .build(boleta.getExternalId()))
                .retrieve()
                .toEntity(byte[].class);

        int codigo = respuesta.getStatusCode().value();
        if (codigo == 404) {
            throw new FactuSmartException(
                    "El archivo todavia no existe. La boleta aun no fue confirmada por SUNAT.", true);
        }
        if (!respuesta.getStatusCode().is2xxSuccessful() || respuesta.getBody() == null) {
            throw fallo(codigo, "descargar el " + tipo.name() + " de la boleta " + boleta.getId(), null);
        }

        String nombre = "%s-%s.%s".formatted(
                boleta.getSerie() == null ? "boleta" : boleta.getSerie(),
                boleta.getCorrelativo() == null ? boleta.getId() : boleta.getCorrelativo(),
                tipo.ruta());
        return new ArchivoComprobante(respuesta.getBody(), tipo.contentType(), nombre);
    }

    @Override
    public int creditosDisponibles() {
        if (config.debeSimular()) {
            return 0;
        }

        ResponseEntity<String> respuesta = factuSmartRestClient.get()
                .uri("/account")
                .retrieve()
                .toEntity(String.class);

        if (!respuesta.getStatusCode().is2xxSuccessful()) {
            throw fallo(respuesta.getStatusCode().value(), "consultar los creditos", respuesta.getBody());
        }

        JsonNode cuerpo = leerJson(respuesta.getBody());
        JsonNode datos = cuerpo.has("creditos_disponibles") ? cuerpo : cuerpo.path("data");
        return datos.path("creditos_disponibles").asInt(0);
    }

    @Override
    public boolean ping() {
        try {
            return factuSmartRestClient.get()
                    .uri("/ping")
                    .retrieve()
                    .toEntity(String.class)
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RuntimeException ex) {
            log.warn("Ping a FactuSmart fallido: {}", ex.getMessage());
            return false;
        }
    }

    // --- Interpretacion de la respuesta ------------------------------------------------

    private FactuSmartRespuesta interpretar(ResponseEntity<String> respuesta, String queSeIntentaba) {
        HttpStatusCode estado = respuesta.getStatusCode();
        if (!estado.is2xxSuccessful()) {
            throw fallo(estado.value(), queSeIntentaba, respuesta.getBody());
        }

        FactuSmartRespuesta resultado = responseMapper.mapear(leerJson(respuesta.getBody()));

        if (resultado.esFalloDeConfiguracionDelRuc()) {
            interruptor.bloquear(resultado.getSunatCodigo(), resultado.getSunatDescripcion());
        }
        if (resultado.getEstadoSunat() == EstadoSunat.ERROR) {
            log.error("SUNAT rechazo al {}: codigo {} - {}. Ese numero quedo quemado, no se reenvia.",
                    queSeIntentaba, resultado.getSunatCodigo(), resultado.getSunatDescripcion());
        }
        return resultado;
    }

    private FactuSmartException fallo(int codigo, String queSeIntentaba, String cuerpo) {
        String detalle = cuerpo == null || cuerpo.isBlank() ? "" : " Detalle: " + recortar(cuerpo);

        return switch (codigo) {
            case 401 -> {
                log.error("API key de FactuSmart invalida al {}.{}", queSeIntentaba, detalle);
                yield new FactuSmartException("La API key de facturacion no es valida", false);
            }
            case 402 -> {
                log.error("SIN CREDITOS en FactuSmart al {}. La venta queda registrada pero sin "
                        + "comprobante hasta que se recarguen.{}", queSeIntentaba, detalle);
                yield new FactuSmartException("No quedan creditos para emitir comprobantes", false);
            }
            case 403 -> {
                log.error("El RUC {} no pertenece a la cuenta de FactuSmart al {}.{}",
                        config.getRuc(), queSeIntentaba, detalle);
                yield new FactuSmartException("El RUC configurado no pertenece a la cuenta", false);
            }
            case 409 -> {
                log.warn("Emision aun en curso al {}. Se reintenta despues.{}", queSeIntentaba, detalle);
                yield new FactuSmartException("Hay una emision en curso para esta boleta", true);
            }
            case 422 -> {
                log.error("FactuSmart rechazo los datos al {}. Reintentar no ayuda, hay que "
                        + "corregir el comprobante.{}", queSeIntentaba, detalle);
                yield new FactuSmartException("Los datos del comprobante no son validos", false);
            }
            case 429 -> {
                log.warn("Demasiadas peticiones a FactuSmart al {}.{}", queSeIntentaba, detalle);
                yield new FactuSmartException("Demasiadas peticiones seguidas, reintentar en un momento", true);
            }
            default -> {
                log.error("FactuSmart respondio {} al {}.{}", codigo, queSeIntentaba, detalle);
                yield new FactuSmartException("El servicio de facturacion no respondio correctamente", true);
            }
        };
    }

    private JsonNode leerJson(String cuerpo) {
        try {
            return objectMapper.readTree(cuerpo == null ? "{}" : cuerpo);
        } catch (Exception ex) {
            throw new FactuSmartException("La respuesta de FactuSmart no es JSON valido", true, ex);
        }
    }

    private void exigirExternalId(Boleta boleta) {
        if (boleta.getExternalId() == null || boleta.getExternalId().isBlank()) {
            throw new FactuSmartException(
                    "La boleta " + boleta.getId() + " no tiene identificador de FactuSmart", false);
        }
    }

    private void esperar() {
        try {
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FactuSmartException("Envio interrumpido", true, ex);
        }
    }

    private String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 500) + "...";
    }

    /**
     * En desarrollo el sistema tiene que seguir siendo usable de punta a punta, asi que se
     * finge una emision aceptada. Va marcada como simulada para que quede registrada como
     * tal en la base y la interfaz no la presente como un comprobante valido. La serie
     * tambien lo grita: SIM1 en vez de B001.
     */
    private FactuSmartRespuesta simular(Boleta boleta) {
        log.warn("FactuSmart en modo simulado: la boleta {} queda como simulada, sin validez fiscal",
                boleta.getId());
        return FactuSmartRespuesta.builder()
                .externalId("SIMULADO-" + boleta.getId())
                .estadoProveedor("05")
                .estadoSunat(EstadoSunat.ACEPTADO)
                .serie("SIM1")
                .correlativo(String.format("%08d", boleta.getId()))
                .simulada(true)
                .build();
    }
}
