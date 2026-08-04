package com.anticucheria.service.impl;

import com.anticucheria.config.CacheConfig;
import com.anticucheria.config.ConsultaDniConfig;
import com.anticucheria.dto.response.ConsultaClienteResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.enums.EstadoConsulta;
import com.anticucheria.service.ConsultaClienteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Verifica a quien pertenece un DNI contra apiperu.dev.
 *
 * Dos advertencias que explican por que esto es solo informativo:
 *
 * 1. El proveedor no consulta RENIEC: lee el padron reducido de SUNAT y otras fuentes
 *    publicas. Un DNI perfectamente valido puede no aparecer, tipicamente el de un menor
 *    de edad. No encontrarlo no significa que este mal.
 * 2. Por proteccion de datos personales no devuelve direccion, solo el nombre.
 *
 * Por eso ningun camino de aca lanza una excepcion hacia la caja. Que el proveedor se
 * caiga, tarde o se quede sin saldo no puede impedir cobrarle a un cliente que ya comio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaClienteServiceImpl implements ConsultaClienteService {

    private static final String NO_SE_PUDO = "No se pudo verificar el DNI en este momento";

    private final ConsultaDniConfig config;
    private final RestClient apiPeruRestClient;
    private final ObjectMapper objectMapper;

    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_DNI, key = "#numero",
            unless = "#result.estado.name() != 'ENCONTRADO'")
    public ConsultaClienteResponse consultar(String tipoDocumento, String numero) {
        if (!TIPO_DNI.equals(tipoDocumento)) {
            throw new ReglaNegocioException("Por ahora solo se puede verificar el DNI");
        }
        if (numero == null || !numero.matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI debe tener 8 digitos");
        }
        if (!config.estaConfigurado()) {
            return noVerificado(numero, "La verificacion de DNI no esta configurada");
        }

        try {
            return interpretar(numero, apiPeruRestClient.post()
                    .uri(config.getUrl())
                    .body(Map.of("dni", numero))
                    .retrieve()
                    .toEntity(String.class));
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar el DNI {}: {}", numero, ex.getMessage());
            return noVerificado(numero, NO_SE_PUDO);
        }
    }

    private ConsultaClienteResponse interpretar(String numero, ResponseEntity<String> respuesta) {
        int codigo = respuesta.getStatusCode().value();

        // El proveedor usa 404 tanto para "ese DNI no figura" como para credenciales
        // malas. El cuerpo distingue: si trae success, la consulta si se proceso.
        JsonNode cuerpo = leerJson(respuesta.getBody());
        boolean respondioLaConsulta = cuerpo.has("success");

        if (codigo == 401 || codigo == 403 || !respondioLaConsulta && !respuesta.getStatusCode().is2xxSuccessful()) {
            log.warn("La consulta de DNI respondio {} y no se pudo verificar el {}", codigo, numero);
            return noVerificado(numero, NO_SE_PUDO);
        }

        String nombre = texto(cuerpo.path("data"), "nombre_completo");
        if (!cuerpo.path("success").asBoolean(false) || nombre == null) {
            return ConsultaClienteResponse.builder()
                    .estado(EstadoConsulta.NO_ENCONTRADO)
                    .numero(numero)
                    .mensaje("No encontramos ese DNI")
                    .build();
        }

        return ConsultaClienteResponse.builder()
                .estado(EstadoConsulta.ENCONTRADO)
                .numero(numero)
                .nombreCompleto(nombre)
                .build();
    }

    private ConsultaClienteResponse noVerificado(String numero, String mensaje) {
        return ConsultaClienteResponse.builder()
                .estado(EstadoConsulta.NO_VERIFICADO)
                .numero(numero)
                .mensaje(mensaje)
                .build();
    }

    private JsonNode leerJson(String cuerpo) {
        try {
            return objectMapper.readTree(cuerpo == null || cuerpo.isBlank() ? "{}" : cuerpo);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        String texto = valor.asText().trim();
        return texto.isBlank() ? null : texto;
    }
}
