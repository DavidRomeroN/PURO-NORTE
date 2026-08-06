package com.anticucheria.realtime;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final MesaRepository mesaRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedido(PedidoActualizadoEvent event) {
        messagingTemplate.convertAndSend(RealtimeTopics.PEDIDOS, event.pedido());
        if (event.afectaParrilla()) {
            messagingTemplate.convertAndSend(RealtimeTopics.PARRILLA, event.pedido());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMesas(MesasActualizadasEvent event) {
        // Lectura post-commit: si se armaba el listado dentro del TX, a veces
        // llegaba el estado OCUPADA aunque liberarMesas ya hubiera corrido.
        var mesas = mesaRepository.findAllByOrderByNumeroAsc().stream()
                .map(CatalogoMapper::toMesaResponse)
                .toList();
        messagingTemplate.convertAndSend(RealtimeTopics.MESAS, mesas);
    }
}
