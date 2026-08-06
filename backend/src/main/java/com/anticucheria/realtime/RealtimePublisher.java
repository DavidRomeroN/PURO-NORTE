package com.anticucheria.realtime;

import com.anticucheria.dto.response.PedidoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    private final ApplicationEventPublisher events;

    public void pedido(PedidoResponse pedido, boolean afectaParrilla, boolean afectaMesas) {
        events.publishEvent(new PedidoActualizadoEvent(pedido, afectaParrilla, afectaMesas));
        if (afectaMesas) {
            mesas();
        }
    }

    public void mesas() {
        events.publishEvent(new MesasActualizadasEvent());
    }
}
