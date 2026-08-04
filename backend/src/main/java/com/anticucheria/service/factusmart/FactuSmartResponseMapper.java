package com.anticucheria.service.factusmart;

import com.anticucheria.model.enums.EstadoSunat;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Traduce la respuesta del proveedor a estado propio.
 *
 * Regla de oro: un HTTP 200 no significa "aceptado por SUNAT". El campo que decide es
 * {@code aceptado_por_sunat}, y el estado "01" es "todavia no confirmado", no un error:
 * FactuSmart reintenta solo y la mayoria se resuelve sin que nadie toque nada. Marcarlo
 * como error haria creer al personal que perdio la venta.
 */
@Component
public class FactuSmartResponseMapper {

    private static final String ACEPTADO_POR_SUNAT = "05";
    private static final String NO_CONFIRMADO_TODAVIA = "01";
    private static final String RECHAZADO_POR_SUNAT = "09";

    public FactuSmartRespuesta mapear(JsonNode cuerpo) {
        JsonNode datos = cuerpo == null ? null : desenvolver(cuerpo);

        String estado = texto(datos, "estado");
        Boolean aceptado = booleanoOpcional(datos, "aceptado_por_sunat");
        String serieNumero = texto(datos, "serie_numero");

        return FactuSmartRespuesta.builder()
                .externalId(texto(datos, "external_id"))
                .estadoProveedor(estado)
                .estadoSunat(traducirEstado(estado, aceptado))
                .serie(parteDeSerieNumero(serieNumero, true))
                .correlativo(parteDeSerieNumero(serieNumero, false))
                .sunatCodigo(texto(datos, "sunat_codigo"))
                .sunatDescripcion(texto(datos, "sunat_descripcion"))
                .build();
    }

    private EstadoSunat traducirEstado(String estado, Boolean aceptado) {
        if (RECHAZADO_POR_SUNAT.equals(estado)) {
            // Ese numero quedo quemado: no se reenvia, se emite uno nuevo corregido.
            return EstadoSunat.ERROR;
        }
        if (ACEPTADO_POR_SUNAT.equals(estado)) {
            // Un "aceptado_por_sunat" explicito en false manda sobre el estado.
            return Boolean.FALSE.equals(aceptado) ? EstadoSunat.PENDIENTE : EstadoSunat.ACEPTADO;
        }
        if (NO_CONFIRMADO_TODAVIA.equals(estado)) {
            return EstadoSunat.PENDIENTE;
        }
        // Estado desconocido: pendiente, que es reintentable y no alarma a nadie.
        return Boolean.TRUE.equals(aceptado) ? EstadoSunat.ACEPTADO : EstadoSunat.PENDIENTE;
    }

    /** "BA01-1318" se parte en serie BA01 y correlativo 1318. */
    private String parteDeSerieNumero(String serieNumero, boolean quiereSerie) {
        if (serieNumero == null || serieNumero.isBlank()) {
            return null;
        }
        int guion = serieNumero.lastIndexOf('-');
        if (guion < 0) {
            return quiereSerie ? serieNumero.trim() : null;
        }
        String parte = quiereSerie
                ? serieNumero.substring(0, guion)
                : serieNumero.substring(guion + 1);
        return parte.isBlank() ? null : parte.trim();
    }

    /** Algunas respuestas traen los datos anidados bajo "data" y otras al ras. */
    private JsonNode desenvolver(JsonNode cuerpo) {
        JsonNode datos = cuerpo.get("data");
        if (datos != null && datos.isObject() && datos.has("estado")) {
            return datos;
        }
        JsonNode documento = cuerpo.get("documento");
        if (documento != null && documento.isObject() && documento.has("estado")) {
            return documento;
        }
        return cuerpo;
    }

    private String texto(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        String texto = valor.asText();
        return texto.isBlank() ? null : texto;
    }

    private Boolean booleanoOpcional(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asBoolean();
    }
}
