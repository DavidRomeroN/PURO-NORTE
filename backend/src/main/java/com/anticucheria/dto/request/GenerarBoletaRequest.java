package com.anticucheria.dto.request;

import com.anticucheria.model.enums.FormaPago;
import com.anticucheria.model.enums.MedioPago;
import com.anticucheria.model.enums.TipoBoleta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerarBoletaRequest {

    @NotNull
    private Long pedidoId;

    @NotNull
    private TipoBoleta tipo;

    @NotNull
    private FormaPago formaPago;

    @NotNull
    private MedioPago medioPago;

    /**
     * Opcional: casi nadie da su DNI en una anticucheria. Solo se vuelve obligatorio
     * cuando la boleta llega a S/700, porque a partir de ahi SUNAT exige identificar
     * al comprador.
     */
    @Pattern(regexp = "\\d{8}", message = "el DNI debe tener 8 digitos")
    private String dniCliente;
}
