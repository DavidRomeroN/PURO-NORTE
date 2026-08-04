package com.anticucheria.service.impl;

import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.Pedido;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoDespacho;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.model.enums.TipoItem;
import com.anticucheria.repository.PedidoItemRepository;
import com.anticucheria.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Despacho de la parrilla")
class DespachoParrillaTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoItemRepository pedidoItemRepository;

    @InjectMocks private PedidoServiceImpl servicio;

    private Pedido pedido;
    private PedidoItem anticucho;
    private PedidoItem bebida;

    @BeforeEach
    void prepararPedido() {
        Usuario mozo = new Usuario();
        mozo.setId(1L);
        mozo.setNombre("Luis");

        Mesa mesa = new Mesa();
        mesa.setId(12L);
        mesa.setNumero(12);

        pedido = new Pedido();
        pedido.setId(50L);
        pedido.setEstado(EstadoPedido.ABIERTO);
        pedido.setMesa(mesa);
        pedido.setMozo(mozo);
        pedido.setMesasUnidas(new ArrayList<>());

        anticucho = item(1L, TipoItem.ANTICUCHO);
        bebida = item(2L, TipoItem.BEBIDA);

        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedido));
        when(pedidoItemRepository.findById(1L)).thenReturn(Optional.of(anticucho));
        when(pedidoItemRepository.findById(2L)).thenReturn(Optional.of(bebida));
        when(pedidoItemRepository.findByPedidoId(50L)).thenReturn(List.of(anticucho, bebida));
    }

    @Test
    @DisplayName("marcar un plato lo saca de pendientes y deja el resto")
    void pasarUnPlato() {
        PedidoResponse respuesta = servicio.marcarDespachoItem(50L, 1L, true);

        assertThat(anticucho.getEstadoDespacho()).isEqualTo(EstadoDespacho.DESPACHADO);
        assertThat(anticucho.getDespachadoEn()).isNotNull();
        assertThat(bebida.getEstadoDespacho()).isEqualTo(EstadoDespacho.PENDIENTE);
        assertThat(respuesta.getPendientesDespacho()).isEqualTo(1);
    }

    @Test
    @DisplayName("se puede deshacer un toque equivocado")
    void deshacer() {
        anticucho.setEstadoDespacho(EstadoDespacho.DESPACHADO);

        PedidoResponse respuesta = servicio.marcarDespachoItem(50L, 1L, false);

        assertThat(anticucho.getEstadoDespacho()).isEqualTo(EstadoDespacho.PENDIENTE);
        assertThat(anticucho.getDespachadoEn()).isNull();
        assertThat(respuesta.getPendientesDespacho()).isEqualTo(2);
    }

    @Test
    @DisplayName("pasar todo marca los pendientes de una sola vez")
    void pasarTodo() {
        anticucho.setEstadoDespacho(EstadoDespacho.DESPACHADO);

        PedidoResponse respuesta = servicio.despacharTodo(50L);

        assertThat(bebida.getEstadoDespacho()).isEqualTo(EstadoDespacho.DESPACHADO);
        assertThat(respuesta.getPendientesDespacho()).isZero();
        verify(pedidoItemRepository).saveAll(any());
    }

    @Test
    @DisplayName("una cuenta cobrada ya no se toca en cocina")
    void cuentaPagada() {
        pedido.setEstado(EstadoPedido.PAGADO);

        assertThatThrownBy(() -> servicio.marcarDespachoItem(50L, 1L, true))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cocina");
    }

    private PedidoItem item(Long id, TipoItem tipo) {
        PedidoItem item = new PedidoItem();
        item.setId(id);
        item.setPedido(pedido);
        item.setTipoItem(tipo);
        item.setCantidad(1);
        item.setPrecioCalculado(BigDecimal.TEN);
        item.setPrecioFinal(BigDecimal.TEN);
        item.setEstadoDespacho(EstadoDespacho.PENDIENTE);
        item.setComponentes(new ArrayList<>());
        return item;
    }
}
