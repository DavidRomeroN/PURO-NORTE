package com.anticucheria.realtime;

import com.anticucheria.dto.response.PedidoResponse;

public record PedidoActualizadoEvent(PedidoResponse pedido, boolean afectaParrilla, boolean afectaMesas) {
}
