package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ComboResponse {

    private Long id;
    private String nombre;
    private BigDecimal precioBase;
    private Boolean activo;
    private List<ComboSlotResponse> slots;
}
