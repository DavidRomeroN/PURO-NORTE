package com.anticucheria.service;

import com.anticucheria.dto.request.AgregarItemRequest;
import com.anticucheria.dto.request.CrearPedidoRequest;
import com.anticucheria.dto.request.EditarPrecioRequest;
import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.model.enums.EstadoPedido;

import java.util.List;


public interface PedidoService {

    PedidoResponse crear(CrearPedidoRequest request, String username);

    PedidoResponse agregarItem(Long pedidoId, AgregarItemRequest request);

    /** Crea varios ítems en una sola transacción y emite un solo evento de tiempo real. */
    PedidoResponse agregarItemsLote(Long pedidoId, List<AgregarItemRequest> items);

    PedidoResponse eliminarItem(Long pedidoId, Long itemId);

    PedidoResponse editarPrecioItem(Long pedidoId, Long itemId, EditarPrecioRequest request, String username);

    PedidoResponse cerrar(Long pedidoId);

    /**
     * Descarta la cuenta sin cobrarla y libera sus mesas. No borra nada: queda como
     * ANULADO con quien lo hizo, cuando y por que.
     */
    PedidoResponse anular(Long pedidoId, String motivo, String username);

    /** El grupo se cambió de mesa: la cuenta pasa a la nueva y las anteriores se liberan. */
    PedidoResponse moverAMesa(Long pedidoId, Long mesaId);

    /** El grupo juntó otra mesa: la cuenta pasa a ocupar las dos. */
    PedidoResponse unirMesa(Long pedidoId, Long mesaId);

    /** Deshace una unión: esa mesa queda libre y la cuenta sigue en las demás. */
    PedidoResponse separarMesa(Long pedidoId, Long mesaId);

    PedidoResponse obtenerPorId(Long id);

    List<PedidoResponse> listar(EstadoPedido estado);

    List<PedidoResponse> listarActivos();

    /**
     * Pedidos vivos para el parrillero. El más viejo primero. Los que ya despacharon
     * todo siguen en la respuesta con pendientesDespacho = 0, para que la UI decida
     * si los oculta o los muestra como listos.
     */
    List<PedidoResponse> listarParaParrilla();

    /** Marca un plato como pasado o lo vuelve a pendiente si se equivocó. */
    PedidoResponse marcarDespachoItem(Long pedidoId, Long itemId, boolean despachado);

    /** Marca todos los platos pendientes del pedido como pasados de una sola vez. */
    PedidoResponse despacharTodo(Long pedidoId);
}
