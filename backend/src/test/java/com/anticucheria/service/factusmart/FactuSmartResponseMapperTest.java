package com.anticucheria.service.factusmart;

import com.anticucheria.model.enums.EstadoSunat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FactuSmartResponseMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FactuSmartResponseMapper mapper = new FactuSmartResponseMapper();
    private final InterruptorDeEmision interruptor = new InterruptorDeEmision();

    @Test
    @DisplayName("estado 05 con aceptado_por_sunat: la boleta tiene validez fiscal")
    void estadoAceptado() {
        FactuSmartRespuesta respuesta = mapear("""
                {
                  "success": true,
                  "external_id": "9f1c-4a2b",
                  "estado": "05",
                  "aceptado_por_sunat": true,
                  "serie_numero": "BA01-1318",
                  "sunat_codigo": "0",
                  "sunat_descripcion": "La Boleta numero BA01-1318, ha sido aceptada"
                }
                """);

        assertThat(respuesta.getEstadoSunat()).isEqualTo(EstadoSunat.ACEPTADO);
        assertThat(respuesta.getExternalId()).isEqualTo("9f1c-4a2b");
        // Lo que viene del proveedor jamas se marca como simulado: esa marca solo la pone
        // el propio sistema cuando finge la emision.
        assertThat(respuesta.isSimulada()).isFalse();
    }

    @Test
    @DisplayName("estado 01 es pendiente, no error: FactuSmart reintenta solo")
    void estadoPendiente() {
        FactuSmartRespuesta respuesta = mapear("""
                { "estado": "01", "aceptado_por_sunat": false, "external_id": "abc" }
                """);

        // Marcarlo como error haria creer al personal que perdio la venta.
        assertThat(respuesta.getEstadoSunat())
                .isEqualTo(EstadoSunat.PENDIENTE)
                .isNotEqualTo(EstadoSunat.ERROR);
    }

    @Test
    @DisplayName("estado 09 es rechazo definitivo: ese numero quedo quemado")
    void estadoRechazado() {
        FactuSmartRespuesta respuesta = mapear("""
                { "estado": "09", "aceptado_por_sunat": false, "sunat_codigo": "1033" }
                """);

        assertThat(respuesta.getEstadoSunat()).isEqualTo(EstadoSunat.ERROR);
    }

    @Test
    @DisplayName("un HTTP 200 con aceptado_por_sunat en false no marca como aceptada")
    void httpCorrectoPeroNoAceptada() {
        // La respuesta llego bien y el proveedor dice 05, pero niega la aceptacion.
        // El campo aceptado_por_sunat es el que manda.
        FactuSmartRespuesta respuesta = mapear("""
                { "success": true, "estado": "05", "aceptado_por_sunat": false }
                """);

        assertThat(respuesta.getEstadoSunat()).isNotEqualTo(EstadoSunat.ACEPTADO);
        assertThat(respuesta.getEstadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);
    }

    @Test
    @DisplayName("serie_numero se parte en serie y correlativo")
    void serieYCorrelativo() {
        FactuSmartRespuesta respuesta = mapear("""
                { "estado": "05", "aceptado_por_sunat": true, "serie_numero": "BA01-1318" }
                """);

        assertThat(respuesta.getSerie()).isEqualTo("BA01");
        assertThat(respuesta.getCorrelativo()).isEqualTo("1318");
    }

    @Test
    @DisplayName("el codigo 0111 suspende la emision para no quemar el dia entero")
    void codigoQueSuspendeLaEmision() {
        FactuSmartRespuesta respuesta = mapear("""
                {
                  "estado": "09",
                  "aceptado_por_sunat": false,
                  "sunat_codigo": "0111",
                  "sunat_descripcion": "El usuario SOL no tiene el perfil para enviar comprobantes"
                }
                """);

        assertThat(respuesta.esFalloDeConfiguracionDelRuc()).isTrue();

        interruptor.bloquear(respuesta.getSunatCodigo(), respuesta.getSunatDescripcion());
        assertThat(interruptor.estaBloqueado()).isTrue();
        assertThat(interruptor.motivo()).contains("0111");

        interruptor.reactivar();
        assertThat(interruptor.estaBloqueado()).isFalse();
    }

    @Test
    @DisplayName("el codigo 0102 tambien suspende, y 1033 no")
    void distingueLosCodigosDeConfiguracion() {
        assertThat(mapear("""
                { "estado": "09", "sunat_codigo": "0102" }
                """).esFalloDeConfiguracionDelRuc()).isTrue();

        // Ese numero quedo quemado, pero los siguientes van a salir bien.
        assertThat(mapear("""
                { "estado": "09", "sunat_codigo": "1033" }
                """).esFalloDeConfiguracionDelRuc()).isFalse();
    }

    @Test
    @DisplayName("tambien lee los datos cuando vienen anidados bajo data")
    void respuestaAnidada() {
        FactuSmartRespuesta respuesta = mapear("""
                {
                  "success": true,
                  "data": {
                    "external_id": "envuelto",
                    "estado": "05",
                    "aceptado_por_sunat": true,
                    "serie_numero": "BA01-7"
                  }
                }
                """);

        assertThat(respuesta.getExternalId()).isEqualTo("envuelto");
        assertThat(respuesta.getEstadoSunat()).isEqualTo(EstadoSunat.ACEPTADO);
        assertThat(respuesta.getCorrelativo()).isEqualTo("7");
    }

    @Test
    @DisplayName("una respuesta vacia o sin estado no se toma por aceptada")
    void respuestaSinEstado() {
        assertThat(mapear("{}").getEstadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);
        assertThat(mapear("{ \"external_id\": \"x\" }").getEstadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);
    }

    private FactuSmartRespuesta mapear(String json) {
        return mapper.mapear(leer(json));
    }

    private JsonNode leer(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON de prueba invalido", ex);
        }
    }
}
