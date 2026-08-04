package com.anticucheria.service.impl;

import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.Pedido;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoMesa;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.repository.PedidoItemRepository;
import com.anticucheria.repository.PedidoRepository;
import com.anticucheria.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Anular la cuenta de una mesa")
class AnularPedidoTest {

    private static final String CAJERA = "rosa";

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoItemRepository pedidoItemRepository;
    @Mock private MesaRepository mesaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PedidoServiceImpl servicio;

    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Rosa");
        when(usuarioRepository.findByUsuario(CAJERA)).thenReturn(Optional.of(usuario));
        when(pedidoItemRepository.findByPedidoId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("libera la mesa principal y las que estaban unidas")
    void liberaTodasLasMesas() {
        Mesa doce = mesa(12L, 12);
        Mesa trece = mesa(13L, 13);
        Pedido pedido = pedido(EstadoPedido.ABIERTO, doce);
        pedido.getMesasUnidas().add(trece);

        servicio.anular(pedido.getId(), "Se fueron sin consumir", CAJERA);

        assertThat(doce.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(trece.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        verify(mesaRepository).save(doce);
        verify(mesaRepository).save(trece);
    }

    @Test
    @DisplayName("deja registrado quién la anuló y por qué")
    void dejaRastro() {
        Pedido pedido = pedido(EstadoPedido.ABIERTO, mesa(12L, 12));

        PedidoResponse respuesta = servicio.anular(pedido.getId(), "Mesa abierta por error", CAJERA);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ANULADO);
        assertThat(pedido.getAnuladoEn()).isNotNull();
        assertThat(pedido.getAnuladoPor()).isSameAs(usuario);
        assertThat(pedido.getMotivoAnulacion()).isEqualTo("Mesa abierta por error");
        assertThat(respuesta.getAnuladoPorNombre()).isEqualTo("Rosa");
        assertThat(respuesta.getMotivoAnulacion()).isEqualTo("Mesa abierta por error");
    }

    /** Una cuenta cerrada sigue ocupando la mesa, así que también hay que poder soltarla. */
    @Test
    @DisplayName("también se puede anular una cuenta ya cerrada sin pagar")
    void cuentaCerradaSinPagar() {
        Mesa doce = mesa(12L, 12);
        Pedido pedido = pedido(EstadoPedido.CERRADO, doce);

        servicio.anular(pedido.getId(), "Se fueron sin pagar", CAJERA);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ANULADO);
        assertThat(doce.getEstado()).isEqualTo(EstadoMesa.LIBRE);
    }

    @Test
    @DisplayName("una cuenta cobrada ya tiene boleta y no se toca")
    void cuentaPagada() {
        Mesa doce = mesa(12L, 12);
        Pedido pedido = pedido(EstadoPedido.PAGADO, doce);

        assertThatThrownBy(() -> servicio.anular(pedido.getId(), "me equivoqué", CAJERA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("boleta");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        verify(mesaRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se anula dos veces")
    void yaAnulada() {
        Pedido pedido = pedido(EstadoPedido.ANULADO, mesa(12L, 12));

        assertThatThrownBy(() -> servicio.anular(pedido.getId(), "otra vez", CAJERA))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("un pedido para llevar no tiene mesa que liberar")
    void pedidoParaLlevar() {
        Pedido pedido = pedido(EstadoPedido.ABIERTO, null);

        servicio.anular(pedido.getId(), "El cliente no vino a recogerlo", CAJERA);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ANULADO);
        verify(mesaRepository, never()).save(any());
    }

    private Mesa mesa(Long id, int numero) {
        Mesa mesa = new Mesa();
        mesa.setId(id);
        mesa.setNumero(numero);
        mesa.setEstado(EstadoMesa.OCUPADA);
        return mesa;
    }

    private Pedido pedido(EstadoPedido estado, Mesa mesa) {
        Pedido pedido = new Pedido();
        pedido.setId(50L);
        pedido.setEstado(estado);
        pedido.setMesa(mesa);
        pedido.setMozo(usuario);
        pedido.setMesasUnidas(new ArrayList<>());
        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedido));
        return pedido;
    }
}
