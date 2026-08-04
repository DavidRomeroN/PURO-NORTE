package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Lo mínimo para nombrar una mesa dentro de un pedido. */
@Getter
@Builder
public class MesaResumenResponse {

    private Long id;
    private Integer numero;
}
