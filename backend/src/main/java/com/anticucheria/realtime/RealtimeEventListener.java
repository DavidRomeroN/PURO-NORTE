package com.anticucheria.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedido(PedidoActualizadoEvent event) {
        messagingTemplate.convertAndSend(RealtimeTopics.PEDIDOS, event.pedido());
        if (event.afectaParrilla()) {
            messagingTemplate.convertAndSend(RealtimeTopics.PARRILLA, event.pedido());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMesas(MesasActualizadasEvent event) {
        messagingTemplate.convertAndSend(RealtimeTopics.MESAS, event.mesas());
    }
}
