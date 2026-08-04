package com.anticucheria.service.factusmart;

import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.enums.TipoItem;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Como se expresa un item del pedido dentro de un comprobante: su descripcion y su
 * codigo de catalogo. Vive aparte porque el detalle guardado en la boleta y el payload
 * que ve SUNAT tienen que decir exactamente lo mismo.
 */
public final class ItemFiscal {

    public static final String CODIGO_CONSUMO = "CONSUMO";
    public static final String DESCRIPCION_CONSUMO = "Consumo";

    private ItemFiscal() {
    }

    public static String descripcion(PedidoItem item) {
        if (item.getTipoItem() == TipoItem.COMBO && item.getCombo() != null) {
            return item.getCombo().getNombre();
        }
        if (!item.getComponentes().isEmpty()) {
            return item.getComponentes().stream()
                    .map(componente -> componente.getProductoBase().getNombre())
                    .collect(Collectors.joining(" + "));
        }
        return item.getTipoItem().name();
    }

    /**
     * Los anticuchos son combinaciones armadas al momento, no productos de catalogo. Se
     * les da una clave derivada de sus componentes, ordenada para que "pollo + carne" y
     * "carne + pollo" caigan en el mismo codigo y no llenen de basura el catalogo del
     * proveedor.
     */
    public static String codigoInterno(PedidoItem item) {
        if (item.getTipoItem() == TipoItem.COMBO && item.getCombo() != null) {
            return "COMBO-" + item.getCombo().getId();
        }

        if (item.getTipoItem() == TipoItem.ANTICUCHO) {
            String ids = item.getComponentes().stream()
                    .map(componente -> componente.getProductoBase().getId())
                    .sorted(Comparator.naturalOrder())
                    .map(String::valueOf)
                    .collect(Collectors.joining("-"));
            return ids.isEmpty() ? "ANT" : "ANT-" + ids;
        }

        return item.getComponentes().stream()
                .findFirst()
                .map(componente -> "PROD-" + componente.getProductoBase().getId())
                .orElse(item.getTipoItem().name());
    }
}
