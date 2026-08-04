package com.anticucheria.dto.response;

import com.anticucheria.model.enums.EstadoMesa;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MesaResponse {

    private Long id;
    private Integer numero;
    private EstadoMesa estado;
}
