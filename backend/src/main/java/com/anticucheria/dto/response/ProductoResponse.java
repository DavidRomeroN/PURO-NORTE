package com.anticucheria.dto.response;

import com.anticucheria.model.enums.TipoProducto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductoResponse {

    private Long id;
    private String nombre;
    private TipoProducto tipo;
    private BigDecimal precioUnitario;
    private Boolean activo;
}
