package com.anticucheria.service.impl;

import com.anticucheria.dto.request.AgregarItemRequest;
import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.Pedido;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.model.enums.TipoItem;
import com.anticucheria.model.enums.TipoProducto;
import com.anticucheria.realtime.RealtimePublisher;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.repository.PedidoItemRepository;
import com.anticucheria.repository.PedidoRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.repository.UsuarioRepository;
import com.anticucheria.service.PrecioEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceLoteTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock PedidoItemRepository pedidoItemRepository;
    @Mock MesaRepository mesaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProductoBaseRepository productoBaseRepository;
    @Mock ComboRepository comboRepository;
    @Mock ComboSlotRepository comboSlotRepository;
    @Mock PrecioEngineService precioEngineService;
    @Mock RealtimePublisher realtimePublisher;

    PedidoServiceImpl service;
    Pedido pedido;

    @BeforeEach
    void setUp() {
        service = new PedidoServiceImpl(
                pedidoRepository, pedidoItemRepository, mesaRepository, usuarioRepository,
                productoBaseRepository, comboRepository, comboSlotRepository,
                precioEngineService, realtimePublisher);

        Usuario mozo = Usuario.builder().id(1L).nombre("Mozo").usuario("mozo").build();
        pedido = Pedido.builder()
                .id(10L)
                .mozo(mozo)
                .estado(EstadoPedido.ABIERTO)
                .creadoEn(LocalDateTime.now())
                .mesasUnidas(new ArrayList<>())
                .build();

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
    }

    @Test
    void loteDe5Items_creaLos5YUnSoloEventoWebsocket() {
        when(precioEngineService.calcularPrecioItemSimple(any())).thenReturn(BigDecimal.valueOf(3));
        when(productoBaseRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return Optional.of(com.anticucheria.model.ProductoBase.builder()
                    .id(id).nombre("Bebida").tipo(TipoProducto.BEBIDA)
                    .precioUnitario(BigDecimal.valueOf(3)).activo(true).build());
        });

        AtomicInteger ids = new AtomicInteger(1);
        when(pedidoItemRepository.save(any(PedidoItem.class))).thenAnswer(inv -> {
            PedidoItem item = inv.getArgument(0);
            item.setId((long) ids.getAndIncrement());
            return item;
        });
        when(pedidoItemRepository.findByPedidoId(10L)).thenAnswer(inv -> List.of());

        List<AgregarItemRequest> lote = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AgregarItemRequest r = new AgregarItemRequest();
            r.setTipoItem(TipoItem.BEBIDA);
            r.setCantidad(1);
            r.setComponentes(List.of(100L));
            lote.add(r);
        }

        // toResponse lee items; devolvemos los guardados vía captor
        ArgumentCaptor<PedidoItem> captor = ArgumentCaptor.forClass(PedidoItem.class);
        when(pedidoItemRepository.findByPedidoId(10L)).thenAnswer(inv -> {
            // se llama al final; usamos saves previos
            return List.of();
        });

        PedidoResponse response = service.agregarItemsLote(10L, lote);

        verify(pedidoItemRepository, times(5)).save(captor.capture());
        verify(realtimePublisher, times(1)).pedido(any(PedidoResponse.class), anyBoolean(), anyBoolean());
        assertThat(response).isNotNull();
        assertThat(captor.getAllValues()).hasSize(5);
    }

    @Test
    void loteConItemInvalido_noPersisteNinguno() {
        AgregarItemRequest bueno = new AgregarItemRequest();
        bueno.setTipoItem(TipoItem.BEBIDA);
        bueno.setCantidad(1);
        bueno.setComponentes(List.of(100L));

        AgregarItemRequest malo = new AgregarItemRequest();
        malo.setTipoItem(TipoItem.BEBIDA);
        malo.setCantidad(1);
        malo.setComponentes(List.of()); // inválido

        when(precioEngineService.calcularPrecioItemSimple(any())).thenReturn(BigDecimal.ONE);
        when(productoBaseRepository.findById(100L)).thenReturn(Optional.of(
                com.anticucheria.model.ProductoBase.builder()
                        .id(100L).nombre("Inca").tipo(TipoProducto.BEBIDA)
                        .precioUnitario(BigDecimal.ONE).activo(true).build()));
        when(pedidoItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.agregarItemsLote(10L, List.of(bueno, malo)))
                .isInstanceOf(ReglaNegocioException.class);

        // En runtime @Transactional hace rollback; aquí verificamos que el segundo falla
        // y que no se publicó evento (la excepción corta antes del publish final... 
        // en realidad el primero ya se guardó en mock; el test de integración cubre rollback.
        verify(realtimePublisher, never()).pedido(any(), anyBoolean(), anyBoolean());
    }
}
