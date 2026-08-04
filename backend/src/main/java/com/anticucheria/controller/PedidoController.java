package com.anticucheria.controller;

import com.anticucheria.dto.request.AgregarItemRequest;
import com.anticucheria.dto.request.AnularPedidoRequest;
import com.anticucheria.dto.request.CambiarMesaRequest;
import com.anticucheria.dto.request.CrearPedidoRequest;
import com.anticucheria.dto.request.DespacharItemRequest;
import com.anticucheria.dto.request.EditarPrecioRequest;
import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request,
                                @AuthenticationPrincipal UserDetails user) {
        return pedidoService.crear(request, user.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public List<PedidoResponse> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidoService.listar(estado);
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public List<PedidoResponse> listarActivos() {
        return pedidoService.listarActivos();
    }

    /**
     * Lista de la parrilla: pedidos vivos del más viejo al más nuevo. La ven los tres
     * roles porque el mozo también consulta qué falta, pero está pensada para la tablet
     * del parrillero.
     */
    @GetMapping("/parrilla")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public List<PedidoResponse> listarParaParrilla() {
        return pedidoService.listarParaParrilla();
    }

    /** Un plato salió, o se deshace el toque si se equivocó. */
    @PatchMapping("/{id}/items/{itemId}/despacho")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse marcarDespachoItem(@PathVariable Long id,
                                             @PathVariable Long itemId,
                                             @Valid @RequestBody DespacharItemRequest request) {
        return pedidoService.marcarDespachoItem(id, itemId, Boolean.TRUE.equals(request.getDespachado()));
    }

    /** Todo el pedido salió de una vez. */
    @PatchMapping("/{id}/despachar")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse despacharTodo(@PathVariable Long id) {
        return pedidoService.despacharTodo(id);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtenerPorId(id);
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse agregarItem(@PathVariable Long id, @Valid @RequestBody AgregarItemRequest request) {
        return pedidoService.agregarItem(id, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse eliminarItem(@PathVariable Long id, @PathVariable Long itemId) {
        return pedidoService.eliminarItem(id, itemId);
    }

    @PatchMapping("/{id}/items/{itemId}/precio")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse editarPrecio(@PathVariable Long id, @PathVariable Long itemId,
                                       @Valid @RequestBody EditarPrecioRequest request,
                                       @AuthenticationPrincipal UserDetails user) {
        return pedidoService.editarPrecioItem(id, itemId, request, user.getUsername());
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('CAJA','ADMIN')")
    public PedidoResponse cerrar(@PathVariable Long id) {
        return pedidoService.cerrar(id);
    }

    /**
     * Los clientes se fueron, o la mesa se abrió por error. El mozo también puede hacerlo
     * porque es quien ve la mesa vacía; por eso el motivo es obligatorio y queda firmado.
     */
    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse anular(@PathVariable Long id,
                                 @Valid @RequestBody AnularPedidoRequest request,
                                 @AuthenticationPrincipal UserDetails user) {
        return pedidoService.anular(id, request.getMotivo(), user.getUsername());
    }

    /** El grupo se cambió de mesa. Lo nota el mozo, así que también puede hacerlo. */
    @PatchMapping("/{id}/mesa")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse moverAMesa(@PathVariable Long id,
                                     @Valid @RequestBody CambiarMesaRequest request) {
        return pedidoService.moverAMesa(id, request.getMesaId());
    }

    @PostMapping("/{id}/mesas")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse unirMesa(@PathVariable Long id,
                                   @Valid @RequestBody CambiarMesaRequest request) {
        return pedidoService.unirMesa(id, request.getMesaId());
    }

    @DeleteMapping("/{id}/mesas/{mesaId}")
    @PreAuthorize("hasAnyRole('MOZO','CAJA','ADMIN')")
    public PedidoResponse separarMesa(@PathVariable Long id, @PathVariable Long mesaId) {
        return pedidoService.separarMesa(id, mesaId);
    }
}
