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
    private BigDecimal precioUnitarioSnapshot;
}
