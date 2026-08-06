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

    String descripcionDe(PedidoItemResponse item) {
        if (item.getTipoItem() == TipoItem.COMBO) {
            return descripcionCombo(item);
        }
        if (item.getComponentes() == null || item.getComponentes().isEmpty()) {
            return item.getTipoItem().name();
        }
        if (item.getTipoItem() == TipoItem.ANTICUCHO) {
            return descripcionAnticucho(item);
        }
        return nombresComponentes(item);
    }

    private static String descripcionAnticucho(PedidoItemResponse item) {
        // Orden de armado (como los eligió el mozo), no por id.
        List<String> nombres = item.getComponentes() == null
                ? List.of()
                : item.getComponentes().stream()
                        .map(ComponenteResponse::getProductoNombre)
                        .filter(Objects::nonNull)
                        .toList();
        if (nombres.isEmpty()) {
            return "Anticucho";
        }
        if (nombres.size() == 1) {
            return "Anticucho de " + lower(nombres.get(0));
        }
        if (nombres.size() == 2) {
            return "doble de " + conY(nombres);
        }
        if (nombres.size() == 3) {
            return "triple de " + conY(nombres);
        }
        return nombres.size() + " palos de " + conY(nombres);
    }

    private static String descripcionCombo(PedidoItemResponse item) {
        String nombre = item.getComboNombre() != null ? item.getComboNombre() : "Mixto";
        List<ComponenteResponse> comps = componentesOrdenados(item);
        List<ComponenteResponse> cambios = comps.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEsSustitucion()))
                .toList();

        if (cambios.size() == 1) {
            ComponenteResponse cambio = cambios.get(0);
            String nuevo = lower(cambio.getProductoNombre());
            String original = lower(cambio.getProductoOriginalNombre());
            if (!nuevo.isEmpty() && !original.isEmpty()) {
                String base = nombre.toLowerCase(Locale.ROOT).contains("especial")
                        ? "mixto especial"
                        : "mixto";
                return base + " " + nuevo + " por " + original;
            }
        }

        if (cambios.size() >= 2) {
            String palitos = nombresComponentes(item);
            return palitos.isEmpty() ? nombre : nombre + " · " + palitos;
        }

        return nombre;
    }

    private static List<ComponenteResponse> componentesOrdenados(PedidoItemResponse item) {
        if (item.getComponentes() == null) {
            return List.of();
        }
        return item.getComponentes().stream()
                .sorted(Comparator
                        .comparing((ComponenteResponse c) -> c.getComboSlotId() == null ? 0L : c.getComboSlotId())
                        .thenComparing(c -> c.getProductoBaseId() == null ? 0L : c.getProductoBaseId()))
                .toList();
    }

    private static List<String> listaNombres(PedidoItemResponse item) {
        return componentesOrdenados(item).stream()
                .map(ComponenteResponse::getProductoNombre)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String nombresComponentes(PedidoItemResponse item) {
        return String.join(" + ", listaNombres(item));
    }

    private static String conY(List<String> nombres) {
        List<String> limpios = nombres.stream().map(AgrupadorItems::lower).filter(s -> !s.isEmpty()).toList();
        if (limpios.isEmpty()) {
            return "";
        }
        if (limpios.size() == 1) {
            return limpios.get(0);
        }
        if (limpios.size() == 2) {
            return limpios.get(0) + " y " + limpios.get(1);
        }
        return String.join(", ", limpios.subList(0, limpios.size() - 1))
                + " y " + limpios.get(limpios.size() - 1);
    }

    private static String lower(String nombre) {
        return nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
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
