package com.anticucheria.service.impl;

import com.anticucheria.dto.request.GenerarBoletaRequest;
import com.anticucheria.dto.response.BoletaDetalleResponse;
import com.anticucheria.dto.response.BoletaResponse;
import com.anticucheria.dto.response.CreditosResponse;
import com.anticucheria.exception.FactuSmartException;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.BoletaDetalle;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.Pedido;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.Usuario;
import com.anticucheria.model.enums.EstadoMesa;
import com.anticucheria.model.enums.EstadoPedido;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.model.enums.TipoBoleta;
import com.anticucheria.repository.BoletaRepository;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.repository.PedidoItemRepository;
import com.anticucheria.repository.PedidoRepository;
import com.anticucheria.repository.UsuarioRepository;
import com.anticucheria.realtime.RealtimePublisher;
import com.anticucheria.service.BoletaService;
import com.anticucheria.service.FactuSmartClientService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.FactuSmartPayloadBuilder;
import com.anticucheria.service.factusmart.FactuSmartRespuesta;
import com.anticucheria.service.factusmart.InterruptorDeEmision;
import com.anticucheria.service.factusmart.ItemFiscal;
import com.anticucheria.service.factusmart.TipoArchivo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoletaServiceImpl implements BoletaService {

    private final BoletaRepository boletaRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final MesaRepository mesaRepository;
    private final FactuSmartClientService factuSmartClientService;
    private final InterruptorDeEmision interruptor;
    private final RealtimePublisher realtimePublisher;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    @Override
    @Transactional
    public BoletaResponse generar(GenerarBoletaRequest request, String cajeroUsername) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + request.getPedidoId()));

        if (pedido.getEstado() != EstadoPedido.ABIERTO && pedido.getEstado() != EstadoPedido.CERRADO) {
            throw new ReglaNegocioException("Solo se puede cobrar una cuenta abierta o cerrada");
        }
        if (boletaRepository.existsByPedidoId(pedido.getId())) {
            throw new ReglaNegocioException("El pedido " + pedido.getId() + " ya tiene una boleta emitida");
        }

        Usuario cajero = usuarioRepository.findByUsuario(cajeroUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Cajero no encontrado: " + cajeroUsername));

        List<PedidoItem> items = pedidoItemRepository.findByPedidoId(pedido.getId());
        if (items.isEmpty()) {
            throw new ReglaNegocioException("No se puede cobrar una cuenta sin ítems");
        }
        BigDecimal montoTotal = items.stream()
                .map(item -> item.getPrecioFinal().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cobro directo: ya no hace falta el paso "cerrar cuenta".
        if (pedido.getEstado() == EstadoPedido.ABIERTO) {
            pedido.setEstado(EstadoPedido.CERRADO);
            pedido.setCerradoEn(LocalDateTime.now());
            pedidoRepository.save(pedido);
        }

        String dni = normalizarDni(request.getDniCliente());
        exigirDniEnBoletasGrandes(montoTotal, dni);

        Boleta boleta = Boleta.builder()
                .pedido(pedido)
                .tipo(request.getTipo())
                .montoTotal(montoTotal)
                .formaPago(request.getFormaPago())
                .medioPago(request.getMedioPago())
                .estadoSunat(EstadoSunat.PENDIENTE)
                .clienteDocumento(dni)
                .cajero(cajero)
                .build();

        if (request.getTipo() == TipoBoleta.DETALLADO) {
            boleta.getDetalles().addAll(construirDetalle(boleta, items));
        }

        // La venta se persiste antes de salir a la red y el envio nunca propaga su
        // excepcion: un rollback aca borraria la boleta recien guardada.
        Boleta guardada = boletaRepository.save(boleta);
        enviar(guardada, () -> factuSmartClientService.emitir(guardada, items));
        boletaRepository.save(guardada);

        // El pedido se cobra igual, haya llegado o no el comprobante a SUNAT.
        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        liberarMesas(pedido);
        asegurarTokenPublico(guardada);
        boletaRepository.save(guardada);

        // Libera mesas en todas las pantallas (bug: mesas no se actualizaban al cobrar).
        realtimePublisher.mesas();

        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoletaResponse> listar(EstadoSunat estadoSunat, LocalDate desde, LocalDate hasta) {
        boolean hayRango = desde != null && hasta != null;
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fin = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        List<Boleta> boletas;
        if (estadoSunat != null && hayRango) {
            boletas = boletaRepository.findByEstadoSunatAndEmitidoEnBetweenOrderByEmitidoEnDesc(estadoSunat, inicio, fin);
        } else if (estadoSunat != null) {
            boletas = boletaRepository.findByEstadoSunatOrderByEmitidoEnDesc(estadoSunat);
        } else if (hayRango) {
            boletas = boletaRepository.findByEmitidoEnBetweenOrderByEmitidoEnDesc(inicio, fin);
        } else {
            boletas = boletaRepository.findAllByOrderByEmitidoEnDesc();
        }

        return boletas.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Override
    @Transactional
    public BoletaResponse reintentar(Long id) {
        Boleta boleta = buscar(id);

        if (boleta.getEstadoSunat() == EstadoSunat.ACEPTADO) {
            throw new ReglaNegocioException("La boleta ya fue aceptada por SUNAT");
        }
        // Un rechazo de SUNAT quema ese numero: reenviarlo no lo arregla, hay que emitir
        // uno nuevo. Si en cambio nunca llego a salir (no hay external_id), no hay nada
        // quemado y reintentar es seguro.
        if (boleta.getEstadoSunat() == EstadoSunat.ERROR && boleta.getExternalId() != null) {
            throw new ReglaNegocioException(
                    "SUNAT rechazo esta boleta y su numero quedo inutilizable. Hay que emitir una nueva.");
        }

        if (boleta.getExternalId() != null) {
            enviar(boleta, () -> factuSmartClientService.reenviar(boleta));
        } else {
            // Nunca se obtuvo respuesta. Se vuelve a emitir con la misma Idempotency-Key,
            // asi que si el primer intento si llego, la API devuelve el mismo comprobante
            // en vez de duplicarlo.
            List<PedidoItem> items = pedidoItemRepository.findByPedidoId(boleta.getPedido().getId());
            enviar(boleta, () -> factuSmartClientService.emitir(boleta, items));
        }

        asegurarTokenPublico(boleta);
        return toResponse(boletaRepository.save(boleta));
    }

    @Override
    @Transactional
    public BoletaResponse sincronizar(Long id) {
        Boleta boleta = buscar(id);
        if (boleta.getExternalId() == null) {
            throw new ReglaNegocioException("Esta boleta nunca llego a enviarse: no hay nada que consultar");
        }

        EstadoSunat antes = boleta.getEstadoSunat();
        enviar(boleta, () -> factuSmartClientService.consultarEnSunat(boleta));
        if (antes != boleta.getEstadoSunat()) {
            log.info("Boleta {}: SUNAT la paso de {} a {}", id, antes, boleta.getEstadoSunat());
        }
        asegurarTokenPublico(boleta);

        return toResponse(boletaRepository.save(boleta));
    }

    @Override
    @Transactional
    public BoletaResponse marcarEnviadaWhatsapp(Long id) {
        Boleta boleta = buscar(id);
        boleta.setEnviadaWhatsapp(true);
        boleta.setEnviadaEn(LocalDateTime.now());
        return toResponse(boletaRepository.save(boleta));
    }

    @Override
    @Transactional
    public void marcarEnviadaCorreo(Long id) {
        Boleta boleta = buscar(id);
        boleta.setEnviadaCorreo(true);
        boleta.setEnviadaEn(LocalDateTime.now());
        boletaRepository.save(boleta);
    }

    // Sin transaccion: bajar un PDF puede tardar y no tiene sentido retener una conexion
    // a la base mientras tanto.
    @Override
    public ArchivoComprobante descargar(Long id, TipoArchivo tipo) {
        Boleta boleta = buscar(id);
        // Se pregunta por la boleta, no por la configuracion actual: una boleta simulada
        // sigue sin tener archivos aunque despues se haya configurado la API key.
        if (boleta.isSimulada()) {
            throw new ReglaNegocioException(
                    "Esta boleta se emitio en modo de prueba, sin conexion con FactuSmart, "
                            + "asi que no hay archivo que mostrar");
        }
        if (boleta.getEstadoSunat() != EstadoSunat.ACEPTADO) {
            throw new ReglaNegocioException("La boleta aun no fue confirmada por SUNAT");
        }
        return factuSmartClientService.descargar(boleta, tipo);
    }

    @Override
    public CreditosResponse creditos() {
        return CreditosResponse.builder()
                .creditosDisponibles(factuSmartClientService.creditosDisponibles())
                .emisionActiva(!interruptor.estaBloqueado())
                .motivoSuspension(interruptor.motivo())
                .build();
    }

    @Override
    public void reactivarEmision() {
        interruptor.reactivar();
    }

    /**
     * Ejecuta una llamada al proveedor y vuelca el resultado sobre la boleta sin dejar
     * escapar la excepcion. Un problema de red o de SUNAT no puede tumbar la venta.
     */
    private void enviar(Boleta boleta, Supplier<FactuSmartRespuesta> llamada) {
        boleta.setIntentosEnvio(boleta.getIntentosEnvio() == null ? 1 : boleta.getIntentosEnvio() + 1);
        boleta.setUltimoIntentoEn(LocalDateTime.now());

        try {
            aplicar(boleta, llamada.get());
        } catch (FactuSmartException ex) {
            // Pendiente si puede resolverse solo; error si necesita que alguien intervenga.
            boleta.setEstadoSunat(ex.isReintentable() ? EstadoSunat.PENDIENTE : EstadoSunat.ERROR);
            boleta.setSunatDescripcion(ex.getMessage());
            log.error("Boleta {}: {}", boleta.getId(), ex.getMessage());
        } catch (RuntimeException ex) {
            boleta.setEstadoSunat(EstadoSunat.PENDIENTE);
            log.error("Boleta {}: fallo inesperado hablando con FactuSmart, queda pendiente",
                    boleta.getId(), ex);
        }
    }

    private void aplicar(Boleta boleta, FactuSmartRespuesta respuesta) {
        if (respuesta.getExternalId() != null) {
            boleta.setExternalId(respuesta.getExternalId());
        }
        if (respuesta.getSerie() != null) {
            boleta.setSerie(respuesta.getSerie());
        }
        if (respuesta.getCorrelativo() != null) {
            boleta.setCorrelativo(respuesta.getCorrelativo());
        }
        boleta.setSunatCodigo(respuesta.getSunatCodigo());
        boleta.setSunatDescripcion(respuesta.getSunatDescripcion());
        boleta.setEstadoSunat(respuesta.getEstadoSunat());
        // Un ACEPTADO simulado y uno real se guardan igual, asi que la marca es lo unico
        // que los distingue despues. Si mas adelante se emite de verdad, se limpia sola.
        boleta.setSimulada(respuesta.isSimulada());
    }

    /**
     * Los pedidos para llevar no ocupan mesa. Y si el grupo habia juntado mesas, se
     * liberan todas: quedarse ocupada una mesa vacia significa no poder sentar clientes.
     */
    private void liberarMesas(Pedido pedido) {
        List<Long> ids = new ArrayList<>();
        if (pedido.getMesa() != null) {
            ids.add(pedido.getMesa().getId());
        }
        for (Mesa unida : pedido.getMesasUnidas()) {
            ids.add(unida.getId());
        }
        for (Long id : ids) {
            mesaRepository.findById(id).ifPresent(mesa -> {
                mesa.setEstado(EstadoMesa.LIBRE);
                mesaRepository.saveAndFlush(mesa);
            });
        }
    }

    private String normalizarDni(String dni) {
        return dni == null || dni.isBlank() ? null : dni.trim();
    }

    private void exigirDniEnBoletasGrandes(BigDecimal montoTotal, String dni) {
        if (dni == null && montoTotal.compareTo(FactuSmartPayloadBuilder.MONTO_QUE_EXIGE_DNI) >= 0) {
            throw new ReglaNegocioException(
                    "Desde S/700 SUNAT exige el DNI del cliente para emitir la boleta");
        }
    }

    private List<BoletaDetalle> construirDetalle(Boleta boleta, List<PedidoItem> items) {
        List<BoletaDetalle> detalles = new ArrayList<>();
        for (PedidoItem item : items) {
            detalles.add(BoletaDetalle.builder()
                    .boleta(boleta)
                    .descripcion(ItemFiscal.descripcion(item))
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioFinal())
                    .subtotal(item.getPrecioFinal().multiply(BigDecimal.valueOf(item.getCantidad())))
                    .build());
        }
        return detalles;
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada: " + id));
    }

    private void asegurarTokenPublico(Boleta boleta) {
        if (boleta.getEstadoSunat() == EstadoSunat.ACEPTADO
                && !boleta.isSimulada()
                && (boleta.getTokenPublico() == null || boleta.getTokenPublico().isBlank())) {
            boleta.setTokenPublico(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private BoletaResponse toResponse(Boleta boleta) {
        boolean aceptada = boleta.getEstadoSunat() == EstadoSunat.ACEPTADO && !boleta.isSimulada();
        String urlPublica = null;
        if (aceptada && boleta.getTokenPublico() != null) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            urlPublica = base + "/api/publico/boletas/" + boleta.getTokenPublico() + "/pdf";
        }
        return BoletaResponse.builder()
                .id(boleta.getId())
                .pedidoId(boleta.getPedido().getId())
                .externalId(boleta.getExternalId())
                .tipo(boleta.getTipo())
                .serie(boleta.getSerie())
                .correlativo(boleta.getCorrelativo())
                .montoTotal(boleta.getMontoTotal())
                .formaPago(boleta.getFormaPago())
                .medioPago(boleta.getMedioPago())
                .estadoSunat(boleta.getEstadoSunat())
                .sunatCodigo(boleta.getSunatCodigo())
                .sunatDescripcion(boleta.getSunatDescripcion())
                .clienteDocumento(boleta.getClienteDocumento())
                .intentosEnvio(boleta.getIntentosEnvio())
                .ultimoIntentoEn(boleta.getUltimoIntentoEn())
                .simulada(boleta.isSimulada())
                // Una boleta simulada figura aceptada pero no existe ningun archivo, asi
                // que la interfaz no debe ofrecer el PDF.
                .descargable(aceptada)
                .urlPublicaPdf(urlPublica)
                .tokenPublico(boleta.getTokenPublico())
                .emitidoEn(boleta.getEmitidoEn())
                .detalles(boleta.getDetalles().stream().map(detalle -> BoletaDetalleResponse.builder()
                        .descripcion(detalle.getDescripcion())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getPrecioUnitario())
                        .subtotal(detalle.getSubtotal())
                        .build()).toList())
                .build();
    }
}
