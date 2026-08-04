package com.anticucheria.service.factusmart;

import com.anticucheria.model.enums.EstadoSunat;
import lombok.Builder;
import lombok.Getter;

/**
 * Lo que FactuSmart contesta sobre un comprobante, ya traducido al vocabulario del
 * sistema. Se construye solo desde el cuerpo de la respuesta: el codigo HTTP no decide
 * nada sobre la validez fiscal.
 */
@Getter
@Builder
public class FactuSmartRespuesta {

    private final String externalId;

    /** Estado crudo del proveedor: "05" aceptado, "01" pendiente, "09" rechazado. */
    private final String estadoProveedor;

    private final EstadoSunat estadoSunat;

    private final String serie;

    private final String correlativo;

    private final String sunatCodigo;

    private final String sunatDescripcion;

    /** Cierto solo si la emision se fingio: nada de esto existe en SUNAT. */
    private final boolean simulada;

    /**
     * Codigos que no se arreglan reintentando porque son configuracion del RUC:
     * 0111 (el usuario SOL no tiene perfil de facturacion electronica) y 0102
     * (usuario o contrasena SOL incorrectos). Con cualquiera de los dos, todas las
     * emisiones siguientes fallarian igual.
     */
    public boolean esFalloDeConfiguracionDelRuc() {
        return "0111".equals(sunatCodigo) || "0102".equals(sunatCodigo);
    }
}
