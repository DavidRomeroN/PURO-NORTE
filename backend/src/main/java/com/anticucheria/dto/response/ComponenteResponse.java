package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComponenteResponse {

    private Long productoBaseId;
    private String productoNombre;
    private Long comboSlotId;
    private Boolean esSustitucion;
    /** Nombre del palito por defecto del slot, solo si hubo sustitución. */
    private String productoOriginalNombre;
    private BigDecimal precioUnitarioSnapshot;
}
