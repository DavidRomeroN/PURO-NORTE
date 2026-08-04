package com.anticucheria.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ComboRequest {

    @NotBlank
    @Size(max = 50)
    private String nombre;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false, message = "debe ser mayor que 0")
    @Digits(integer = 4, fraction = 2, message = "admite máximo 2 decimales")
    private BigDecimal precioBase;
}
