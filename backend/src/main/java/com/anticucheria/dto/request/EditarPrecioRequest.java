package com.anticucheria.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EditarPrecioRequest {

    @NotNull
    @DecimalMin(value = "0.00", message = "no puede ser negativo")
    @Digits(integer = 4, fraction = 2, message = "admite máximo 2 decimales")
    private BigDecimal precioFinal;

    /** Opcional: la cajera no siempre tiene tiempo de escribirlo. */
    @Size(max = 255)
    private String motivo;
}
