package com.anticucheria.realtime;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    private final ApplicationEventPublisher events;
    private final MesaRepository mesaRepository;

    public void pedido(PedidoResponse pedido, boolean afectaParrilla, boolean afectaMesas) {
        events.publishEvent(new PedidoActualizadoEvent(pedido, afectaParrilla, afectaMesas));
        if (afectaMesas) {
            publicarMesas();
        }
    }

    public void mesas() {
        publicarMesas();
    }

    private void publicarMesas() {
        events.publishEvent(new MesasActualizadasEvent(
                mesaRepository.findAllByOrderByNumeroAsc().stream()
                        .map(CatalogoMapper::toMesaResponse)
                        .toList()));
    }
}
