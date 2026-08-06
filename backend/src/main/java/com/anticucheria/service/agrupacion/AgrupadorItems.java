package com.anticucheria.service.agrupacion;

import com.anticucheria.dto.response.ComponenteResponse;
import com.anticucheria.dto.response.PedidoItemResponse;
import com.anticucheria.model.enums.TipoItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Agrupa ítems idénticos para cocina y canasta. Criterios: tipo, componentes
 * (IDs ordenados), sustituciones de combo, comentario y para llevar.
 */
@Component
public class AgrupadorItems {

    public List<LineaAgrupada> agrupar(List<PedidoItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, List<PedidoItemResponse>> porClave = new LinkedHashMap<>();
        for (PedidoItemResponse item : items) {
            porClave.computeIfAbsent(claveDe(item), k -> new ArrayList<>()).add(item);
        }

        List<LineaAgrupada> conComentario = new ArrayList<>();
        List<LineaAgrupada> sinComentario = new ArrayList<>();

        for (Map.Entry<String, List<PedidoItemResponse>> entrada : porClave.entrySet()) {
            List<PedidoItemResponse> grupo = entrada.getValue();
            PedidoItemResponse muestra = grupo.get(0);
            int cantidad = grupo.stream().mapToInt(PedidoItemResponse::getCantidad).sum();
            LineaAgrupada linea = LineaAgrupada.builder()
                    .clave(entrada.getKey())
                    .descripcion(descripcionDe(muestra))
                    .cantidad(cantidad)
                    .observacion(normalizarObs(muestra.getObservaciones()))
                    .paraLlevar(Boolean.TRUE.equals(muestra.getParaLlevar()))
                    .items(List.copyOf(grupo))
                    .build();
            if (linea.getObservacion() != null) {
                conComentario.add(linea);
            } else {
                sinComentario.add(linea);
            }
        }

        List<LineaAgrupada> resultado = new ArrayList<>(sinComentario);
        resultado.addAll(conComentario);
        return resultado;
    }

    public String claveDe(PedidoItemResponse item) {
        TipoItem tipo = item.getTipoItem();
        String componentes = item.getComponentes() == null ? "" : item.getComponentes().stream()
                .sorted(Comparator
                        .comparing((ComponenteResponse c) -> c.getComboSlotId() == null ? 0L : c.getComboSlotId())
                        .thenComparing(c -> c.getProductoBaseId() == null ? 0L : c.getProductoBaseId()))
                .map(c -> {
                    String prod = String.valueOf(c.getProductoBaseId());
                    if (Boolean.TRUE.equals(c.getEsSustitucion()) && c.getComboSlotId() != null) {
                        return "slot" + c.getComboSlotId() + "=" + prod;
                    }
                    return prod;
                })
                .collect(Collectors.joining("-"));

        String combo = item.getComboId() == null ? "" : String.valueOf(item.getComboId());
        String obs = normalizarObs(item.getObservaciones());
        String obsClave = obs == null ? "null" : slug(obs);
        boolean llevar = Boolean.TRUE.equals(item.getParaLlevar());

        if (tipo == TipoItem.COMBO) {
            return "COMBO|" + combo + "|" + componentes + "|" + obsClave + "|" + llevar;
        }
        return tipo.name() + "|" + componentes + "|" + obsClave + "|" + llevar;
    }

    private String descripcionDe(PedidoItemResponse item) {
        if (item.getTipoItem() == TipoItem.COMBO) {
            String nombre = item.getComboNombre() != null ? item.getComboNombre() : "Combo";
            String palitos = nombresComponentes(item);
            return palitos.isEmpty() ? nombre : nombre + " · " + palitos;
        }
        if (item.getComponentes() == null || item.getComponentes().isEmpty()) {
            return item.getTipoItem().name();
        }
        String nombres = nombresComponentes(item);
        if (item.getTipoItem() == TipoItem.ANTICUCHO) {
            return item.getComponentes().size() == 1
                    ? "Anticucho de " + nombres.toLowerCase(Locale.ROOT)
                    : nombres;
        }
        return nombres;
    }

    private static String nombresComponentes(PedidoItemResponse item) {
        if (item.getComponentes() == null || item.getComponentes().isEmpty()) {
            return "";
        }
        return item.getComponentes().stream()
                .sorted(Comparator
                        .comparing((ComponenteResponse c) -> c.getComboSlotId() == null ? 0L : c.getComboSlotId())
                        .thenComparing(c -> c.getProductoBaseId() == null ? 0L : c.getProductoBaseId()))
                .map(ComponenteResponse::getProductoNombre)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" + "));
    }

    private static String normalizarObs(String obs) {
        if (obs == null) {
            return null;
        }
        String t = obs.trim();
        return t.isEmpty() ? null : t;
    }

    private static String slug(String texto) {
        return texto.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }
}
