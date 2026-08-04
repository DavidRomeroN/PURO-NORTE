package com.anticucheria.dto.response;

import com.anticucheria.model.enums.EstadoConsulta;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultaClienteResponse {

    private EstadoConsulta estado;

    private String numero;

    /** Solo cuando se encontro. Es informativo: no viaja en la boleta. */
    private String nombreCompleto;

    /** Que decirle al cajero cuando no hay nombre que mostrar. */
    private String mensaje;
}
