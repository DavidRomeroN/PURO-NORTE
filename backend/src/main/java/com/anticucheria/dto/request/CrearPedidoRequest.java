package com.anticucheria.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearPedidoRequest {

    /** Nulo cuando el pedido es para llevar y por lo tanto no ocupa ninguna mesa. */
    private Long mesaId;
}
