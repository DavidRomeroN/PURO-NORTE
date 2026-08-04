package com.anticucheria.service.impl;

import com.anticucheria.dto.request.AgregarItemRequest;
import com.anticucheria.dto.request.CrearPedidoRequest;
import com.anticucheria.dto.request.EditarPrecioRequest;
import com.anticucheria.dto.request.SustitucionDTO;
import com.anticucheria.dto.response.ComponenteResponse;
import com.anticucheria.dto.response.MesaResumenResponse;
import com.anticucheria.dto.response.PedidoItemResponse;
import com.anticucheria.dto.response.PedidoResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.Pedido;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.PedidoItemComponente;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoDespacho;
import com.anticucheria.model.enums.EstadoMesa;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.model.enums.TipoItem;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.repository.PedidoItemRepository;
import com.anticucheria.repository.PedidoRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.repository.UsuarioRepository;
import com.anticucheria.service.PedidoService;
import com.anticucheria.service.PrecioEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoBaseRepository productoBaseRepository;
    private final ComboRepository comboRepository;
    private final ComboSlotRepository comboSlotRepository;
    private final PrecioEngineService precioEngineService;

    @Override
    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request, String username) {
        Usuario mozo = buscarUsuario(username);

        if (request.getMesaId() == null) {
            return toResponse(crearParaLlevar(mozo));
        }

        Mesa mesa = buscarMesa(request.getMesaId());
        // Cuenta la mesa como ocupada aunque la cuenta esté cerrada sin pagar, y también
        // si está unida a otra cuenta: en los dos casos el grupo sigue sentado ahí.
        exigirMesaLibre(mesa);

        Pedido pedido = pedidoRepository.save(Pedido.builder()
                .mesa(mesa)
                .mozo(mozo)
                .estado(EstadoPedido.ABIERTO)
                .build());

        ocupar(mesa);

        return toResponse(pedido);
    }

    private Pedido crearParaLlevar(Usuario mozo) {
        LocalDate hoy = LocalDate.now();
        int numero = pedidoRepository.ultimoNumeroLlevar(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX)) + 1;

        return pedidoRepository.save(Pedido.builder()
                .mozo(mozo)
                .numeroLlevar(numero)
                .estado(EstadoPedido.ABIERTO)
                .build());
    }

    @Override
    @Transactional
    public PedidoResponse agregarItem(Long pedidoId, AgregarItemRequest request) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new ReglaNegocioException("Solo se pueden agregar ítems a pedidos abiertos");
        }

        BigDecimal precioUnitario = calcularPrecio(request);

        PedidoItem item = PedidoItem.builder()
                .pedido(pedido)
                .tipoItem(request.getTipoItem())
                .cantidad(request.getCantidad())
                .precioCalculado(precioUnitario)
                .precioFinal(precioUnitario)
                .paraLlevar(Boolean.TRUE.equals(request.getParaLlevar()))
                .observaciones(request.getObservaciones())
                .build();

        if (request.getTipoItem() == TipoItem.COMBO) {
            Combo combo = comboRepository.findById(request.getComboId())
                    .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado: " + request.getComboId()));
            item.setCombo(combo);
            item.getComponentes().addAll(componentesDeCombo(item, combo.getId(), request.getSustituciones()));
        } else {
            item.getComponentes().addAll(componentesSueltos(item, request.getComponentes()));
        }

        pedidoItemRepository.save(item);
        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse eliminarItem(Long pedidoId, Long itemId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new ReglaNegocioException("Solo se pueden quitar ítems de pedidos abiertos");
        }

        PedidoItem item = buscarItem(pedidoId, itemId);
        pedidoItemRepository.delete(item);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse editarPrecioItem(Long pedidoId, Long itemId, EditarPrecioRequest request, String username) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new ReglaNegocioException("No se puede editar el precio de un pedido ya pagado");
        }

        PedidoItem item = buscarItem(pedidoId, itemId);

        item.setPrecioFinal(request.getPrecioFinal());
        item.setEditadoManualmente(true);
        item.setEditadoPor(buscarUsuario(username));
        item.setMotivoEdicion(request.getMotivo());
        pedidoItemRepository.save(item);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cerrar(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new ReglaNegocioException("El pedido ya está cerrado o pagado");
        }
        if (pedidoItemRepository.findByPedidoId(pedidoId).isEmpty()) {
            throw new ReglaNegocioException("No se puede cerrar un pedido sin ítems");
        }

        pedido.setEstado(EstadoPedido.CERRADO);
        pedido.setCerradoEn(LocalDateTime.now());
        pedidoRepository.save(pedido);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse anular(Long pedidoId, String motivo, String username) {
        Pedido pedido = buscarPedido(pedidoId);

        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new ReglaNegocioException(
                    "Esta cuenta ya se cobró y tiene boleta. Para deshacerla hay que anular el comprobante.");
        }
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new ReglaNegocioException("Esta cuenta ya estaba anulada");
        }

        BigDecimal total = totalDe(pedidoId);
        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setAnuladoEn(LocalDateTime.now());
        pedido.setAnuladoPor(buscarUsuario(username));
        pedido.setMotivoAnulacion(motivo);
        pedidoRepository.save(pedido);

        // Las mesas quedan libres, que es el punto: si no, se pierden para todo el día.
        // Las filas de mesas unidas se dejan como historia; una cuenta anulada ya no
        // cuenta como ocupante.
        liberarTodas(pedido);

        if (total.signum() > 0) {
            // Anular con consumo es la forma más cómoda de que una mesa no pase por caja.
            // Que quede en el log además de en la base, para poder cruzarlo después.
            log.warn("Cuenta {} anulada por {} con S/{} consumidos. Motivo: {}",
                    pedidoId, username, total, motivo);
        }

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse moverAMesa(Long pedidoId, Long mesaId) {
        Pedido pedido = exigirCuentaEnMesa(pedidoId);
        Mesa destino = buscarMesa(mesaId);

        if (destino.getId().equals(pedido.getMesa().getId())) {
            throw new ReglaNegocioException("La cuenta ya está en la mesa " + destino.getNumero());
        }

        // El destino puede ser una mesa que la cuenta ya tenía unida: el grupo se juntó
        // en la 13 y dejó libre la 12. Ahí no hay que exigir que esté libre, porque ya
        // es de esta misma cuenta.
        boolean eraUnidaDeEstaCuenta = pedido.getMesasUnidas()
                .removeIf(mesa -> mesa.getId().equals(destino.getId()));
        if (!eraUnidaDeEstaCuenta) {
            exigirMesaLibre(destino);
        }

        // Se mueve el grupo entero: si tenía otras mesas unidas, también las deja.
        liberar(pedido.getMesa());
        pedido.getMesasUnidas().forEach(this::liberar);
        pedido.getMesasUnidas().clear();

        pedido.setMesa(destino);
        ocupar(destino);
        pedidoRepository.save(pedido);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse unirMesa(Long pedidoId, Long mesaId) {
        Pedido pedido = exigirCuentaEnMesa(pedidoId);
        Mesa nueva = buscarMesa(mesaId);

        if (nueva.getId().equals(pedido.getMesa().getId())
                || pedido.getMesasUnidas().stream().anyMatch(m -> m.getId().equals(nueva.getId()))) {
            throw new ReglaNegocioException("La mesa " + nueva.getNumero() + " ya es parte de esta cuenta");
        }
        exigirMesaLibre(nueva);

        pedido.getMesasUnidas().add(nueva);
        ocupar(nueva);
        pedidoRepository.save(pedido);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse separarMesa(Long pedidoId, Long mesaId) {
        Pedido pedido = exigirCuentaEnMesa(pedidoId);

        Mesa unida = pedido.getMesasUnidas().stream()
                .filter(mesa -> mesa.getId().equals(mesaId))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "Esa mesa no está unida a esta cuenta. Para dejar la mesa principal, "
                                + "cambia la cuenta a otra mesa."));

        pedido.getMesasUnidas().remove(unida);
        liberar(unida);
        pedidoRepository.save(pedido);

        return toResponse(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        return toResponse(buscarPedido(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(EstadoPedido estado) {
        List<Pedido> pedidos = estado != null
                ? pedidoRepository.findByEstadoOrderByCreadoEnDesc(estado)
                : pedidoRepository.findAllByOrderByCreadoEnDesc();
        return pedidos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarActivos() {
        return listar(EstadoPedido.ABIERTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarParaParrilla() {
        return pedidoRepository.findParaParrilla().stream()
                .map(this::toResponse)
                .filter(pedido -> !pedido.getItems().isEmpty())
                .toList();
    }

    @Override
    @Transactional
    public PedidoResponse marcarDespachoItem(Long pedidoId, Long itemId, boolean despachado) {
        Pedido pedido = exigirPedidoVivoParaDespacho(pedidoId);
        PedidoItem item = buscarItem(pedidoId, itemId);

        if (despachado) {
            item.setEstadoDespacho(EstadoDespacho.DESPACHADO);
            item.setDespachadoEn(LocalDateTime.now());
        } else {
            item.setEstadoDespacho(EstadoDespacho.PENDIENTE);
            item.setDespachadoEn(null);
        }
        pedidoItemRepository.save(item);

        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse despacharTodo(Long pedidoId) {
        Pedido pedido = exigirPedidoVivoParaDespacho(pedidoId);
        LocalDateTime ahora = LocalDateTime.now();

        List<PedidoItem> items = pedidoItemRepository.findByPedidoId(pedidoId);
        for (PedidoItem item : items) {
            if (item.getEstadoDespacho() != EstadoDespacho.DESPACHADO) {
                item.setEstadoDespacho(EstadoDespacho.DESPACHADO);
                item.setDespachadoEn(ahora);
            }
        }
        pedidoItemRepository.saveAll(items);

        return toResponse(pedido);
    }

    /**
     * La parrilla solo trabaja con cuentas vivas. Pagado y anulado ya no tienen platos
     * que sacar; si se anula, los pendientes desaparecen solos de la lista.
     */
    private Pedido exigirPedidoVivoParaDespacho(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() == EstadoPedido.PAGADO || pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new ReglaNegocioException("Esta cuenta ya no está en cocina");
        }
        return pedido;
    }

    private BigDecimal calcularPrecio(AgregarItemRequest request) {
        return switch (request.getTipoItem()) {
            case ANTICUCHO -> {
                exigirComponentes(request);
                yield precioEngineService.calcularPrecioAnticucho(request.getComponentes());
            }
            case COMBO -> {
                if (request.getComboId() == null) {
                    throw new ReglaNegocioException("Un ítem de tipo COMBO requiere comboId");
                }
                yield precioEngineService.calcularPrecioCombo(request.getComboId(), request.getSustituciones());
            }
            case BEBIDA, EXTRA -> {
                exigirComponentes(request);
                if (request.getComponentes().size() != 1) {
                    throw new ReglaNegocioException("Una bebida o extra requiere exactamente un producto");
                }
                yield precioEngineService.calcularPrecioItemSimple(request.getComponentes().get(0));
            }
        };
    }

    private void exigirComponentes(AgregarItemRequest request) {
        if (request.getComponentes() == null || request.getComponentes().isEmpty()) {
            throw new ReglaNegocioException("Debe indicar al menos un componente");
        }
    }

    private List<PedidoItemComponente> componentesSueltos(PedidoItem item, List<Long> productoBaseIds) {
        List<PedidoItemComponente> componentes = new ArrayList<>();
        for (Long productoId : productoBaseIds) {
            ProductoBase producto = buscarProductoActivo(productoId);
            componentes.add(PedidoItemComponente.builder()
                    .pedidoItem(item)
                    .productoBase(producto)
                    .esSustitucion(false)
                    .precioUnitarioSnapshot(producto.getPrecioUnitario())
                    .build());
        }
        return componentes;
    }

    private List<PedidoItemComponente> componentesDeCombo(PedidoItem item, Long comboId,
                                                          List<SustitucionDTO> sustituciones) {
        List<ComboSlot> slots = comboSlotRepository.findByComboIdOrderByOrdenAsc(comboId);
        Map<Long, SustitucionDTO> porSlot = sustituciones == null
                ? Map.of()
                : sustituciones.stream()
                        .collect(Collectors.toMap(SustitucionDTO::getComboSlotId, Function.identity(), (a, b) -> b));

        List<PedidoItemComponente> componentes = new ArrayList<>();
        for (ComboSlot slot : slots) {
            SustitucionDTO sustitucion = porSlot.get(slot.getId());
            ProductoBase producto;
            boolean esSustitucion = false;

            if (sustitucion != null) {
                producto = buscarProductoActivo(sustitucion.getProductoBaseNuevoId());
                esSustitucion = !producto.getId().equals(slot.getProductoBaseDefault().getId());
            } else {
                producto = slot.getProductoBaseDefault();
                exigirActivo(producto);
            }

            componentes.add(PedidoItemComponente.builder()
                    .pedidoItem(item)
                    .productoBase(producto)
                    .comboSlot(slot)
                    .esSustitucion(esSustitucion)
                    .precioUnitarioSnapshot(producto.getPrecioUnitario())
                    .build());
        }
        return componentes;
    }

    private ProductoBase buscarProductoActivo(Long productoId) {
        ProductoBase producto = productoBaseRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        exigirActivo(producto);
        return producto;
    }

    private void exigirActivo(ProductoBase producto) {
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' no está disponible");
        }
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
    }

    private Mesa buscarMesa(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
    }

    /** Reglas comunes a mover, unir y separar. */
    private Pedido exigirCuentaEnMesa(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);

        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new ReglaNegocioException("Esta cuenta ya fue cobrada");
        }
        if (pedido.getMesa() == null) {
            throw new ReglaNegocioException("Un pedido para llevar no ocupa mesa");
        }
        return pedido;
    }

    private void exigirMesaLibre(Mesa mesa) {
        List<Pedido> ocupantes = pedidoRepository.cuentasVivasEnMesa(mesa.getId());
        if (!ocupantes.isEmpty()) {
            throw new ReglaNegocioException("La mesa " + mesa.getNumero()
                    + " ya tiene una cuenta abierta. Cóbrala primero.");
        }
    }

    private void ocupar(Mesa mesa) {
        mesa.setEstado(EstadoMesa.OCUPADA);
        mesaRepository.save(mesa);
    }

    private void liberar(Mesa mesa) {
        mesa.setEstado(EstadoMesa.LIBRE);
        mesaRepository.save(mesa);
    }

    /** Un pedido para llevar no tiene mesa; uno de salón puede tener varias unidas. */
    private void liberarTodas(Pedido pedido) {
        if (pedido.getMesa() != null) {
            liberar(pedido.getMesa());
        }
        pedido.getMesasUnidas().forEach(this::liberar);
    }

    private BigDecimal totalDe(Long pedidoId) {
        return pedidoItemRepository.findByPedidoId(pedidoId).stream()
                .map(item -> item.getPrecioFinal().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoItem buscarItem(Long pedidoId, Long itemId) {
        PedidoItem item = pedidoItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado: " + itemId));
        if (!item.getPedido().getId().equals(pedidoId)) {
            throw new ReglaNegocioException("El ítem no pertenece al pedido indicado");
        }
        return item;
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<PedidoItem> items = pedidoItemRepository.findByPedidoId(pedido.getId());
        BigDecimal total = items.stream()
                .map(i -> i.getPrecioFinal().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Mesa mesa = pedido.getMesa();

        return PedidoResponse.builder()
                .id(pedido.getId())
                .mesaId(mesa != null ? mesa.getId() : null)
                .mesaNumero(mesa != null ? mesa.getNumero() : null)
                .mesasUnidas(pedido.getMesasUnidas().stream()
                        .map(unida -> MesaResumenResponse.builder()
                                .id(unida.getId())
                                .numero(unida.getNumero())
                                .build())
                        .toList())
                .numeroLlevar(pedido.getNumeroLlevar())
                .mozoId(pedido.getMozo().getId())
                .mozoNombre(pedido.getMozo().getNombre())
                .estado(pedido.getEstado())
                .creadoEn(pedido.getCreadoEn())
                .cerradoEn(pedido.getCerradoEn())
                .anuladoEn(pedido.getAnuladoEn())
                .anuladoPorNombre(pedido.getAnuladoPor() != null ? pedido.getAnuladoPor().getNombre() : null)
                .motivoAnulacion(pedido.getMotivoAnulacion())
                .total(total)
                .pendientesDespacho((int) items.stream()
                        .filter(item -> item.getEstadoDespacho() != EstadoDespacho.DESPACHADO)
                        .count())
                .items(items.stream().map(this::toItemResponse).toList())
                .build();
    }

    private PedidoItemResponse toItemResponse(PedidoItem item) {
        return PedidoItemResponse.builder()
                .id(item.getId())
                .tipoItem(item.getTipoItem())
                .comboId(item.getCombo() != null ? item.getCombo().getId() : null)
                .comboNombre(item.getCombo() != null ? item.getCombo().getNombre() : null)
                .cantidad(item.getCantidad())
                .precioCalculado(item.getPrecioCalculado())
                .precioFinal(item.getPrecioFinal())
                .editadoManualmente(item.getEditadoManualmente())
                .motivoEdicion(item.getMotivoEdicion())
                .paraLlevar(item.getParaLlevar())
                .estadoDespacho(item.getEstadoDespacho())
                .despachadoEn(item.getDespachadoEn())
                .observaciones(item.getObservaciones())
                .componentes(item.getComponentes().stream().map(c -> ComponenteResponse.builder()
                        .productoBaseId(c.getProductoBase().getId())
                        .productoNombre(c.getProductoBase().getNombre())
                        .comboSlotId(c.getComboSlot() != null ? c.getComboSlot().getId() : null)
                        .esSustitucion(c.getEsSustitucion())
                        .precioUnitarioSnapshot(c.getPrecioUnitarioSnapshot())
                        .build()).toList())
                .build();
    }
}
