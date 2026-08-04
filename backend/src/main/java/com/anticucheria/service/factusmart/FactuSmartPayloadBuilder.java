package com.anticucheria.service.factusmart;

import com.anticucheria.config.FactuSmartConfig;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.enums.TipoBoleta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el JSON que espera FactuSmart para una boleta de venta.
 *
 * El punto delicado es el IGV. Los precios del negocio (S/17 el mixto) son finales al
 * publico, con IGV incluido, y la API espera el valor sin IGV y calcula hacia arriba.
 * Al despejar hacia atras aparecen descuadres de un centimo en algunos montos: con
 * S/6.00, la base redondeada da 5.08 y el IGV 0.91, que reconstruye 5.99 y no 6.00.
 *
 * Se implementa el algoritmo de referencia que publica la propia documentacion del
 * proveedor, porque es el que su validacion espera. Si la prueba en sandbox muestra que
 * el descuadre no pasa, {@code factusmart.igv-por-resta=true} calcula el IGV restando la
 * base al total cobrado para que cuadre exacto.
 *
 * Todo con BigDecimal: con double, un cobro de S/17.00 se convierte en 16.999999.
 */
@Component
@RequiredArgsConstructor
public class FactuSmartPayloadBuilder {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Boleta de venta. Facturas y notas de credito quedan fuera del alcance. */
    private static final String TIPO_DOCUMENTO_BOLETA = "03";
    private static final String TIPO_OPERACION_VENTA_INTERNA = "0101";
    private static final String MONEDA_SOLES = "PEN";
    private static final String UNIDAD_MEDIDA = "NIU";
    private static final String TIPO_PRECIO_VENTA = "01";

    /**
     * Gravado, 18%. La afectacion describe al PRODUCTO, no al regimen tributario del
     * emisor: los anticuchos son bienes gravados normales, aunque el RUC este en NRUS.
     */
    private static final String AFECTACION_GRAVADO = "10";

    private static final String DOC_IDENTIDAD_DNI = "1";
    private static final String DOC_IDENTIDAD_SIN_DOCUMENTO = "0";
    private static final String DOC_ANONIMO = "00000000";
    private static final String NOMBRE_ANONIMO = "CLIENTE VARIOS";

    /** SUNAT exige identificar al comprador desde este monto. */
    public static final BigDecimal MONTO_QUE_EXIGE_DNI = new BigDecimal("700.00");

    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final FactuSmartConfig config;
    private final Clock reloj;

    public Map<String, Object> construir(Boleta boleta, List<PedidoItem> items) {
        List<Map<String, Object>> lineas = boleta.getTipo() == TipoBoleta.CONSUMO
                ? List.of(lineaDeConsumo(boleta))
                : lineasDetalladas(items);

        LocalDate hoy = LocalDate.now(reloj);
        LocalTime ahora = LocalTime.now(reloj);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruc", config.getRuc());
        // La serie de una boleta la asigna FactuSmart; solo se pide el correlativo.
        payload.put("numero_documento", "#");
        payload.put("fecha_de_emision", hoy.format(FECHA));
        payload.put("hora_de_emision", ahora.format(HORA));
        payload.put("codigo_tipo_operacion", TIPO_OPERACION_VENTA_INTERNA);
        payload.put("codigo_tipo_documento", TIPO_DOCUMENTO_BOLETA);
        payload.put("codigo_tipo_moneda", MONEDA_SOLES);
        payload.put("datos_del_cliente_o_receptor", datosDelCliente(boleta.getClienteDocumento()));
        payload.put("totales", totales(lineas));
        payload.put("items", lineas);
        return payload;
    }

    private Map<String, Object> datosDelCliente(String dni) {
        Map<String, Object> cliente = new LinkedHashMap<>();
        if (dni != null && !dni.isBlank()) {
            // Con el DNI basta: la API completa el nombre desde RENIEC.
            cliente.put("codigo_tipo_documento_identidad", DOC_IDENTIDAD_DNI);
            cliente.put("numero_documento", dni.trim());
            return cliente;
        }

        cliente.put("codigo_tipo_documento_identidad", DOC_IDENTIDAD_SIN_DOCUMENTO);
        cliente.put("numero_documento", DOC_ANONIMO);
        cliente.put("apellidos_y_nombres_o_razon_social", NOMBRE_ANONIMO);
        return cliente;
    }

    private Map<String, Object> lineaDeConsumo(Boleta boleta) {
        return linea(ItemFiscal.CODIGO_CONSUMO, ItemFiscal.DESCRIPCION_CONSUMO, 1, boleta.getMontoTotal());
    }

    private List<Map<String, Object>> lineasDetalladas(List<PedidoItem> items) {
        List<Map<String, Object>> lineas = new ArrayList<>();
        for (PedidoItem item : items) {
            lineas.add(linea(
                    ItemFiscal.codigoInterno(item),
                    ItemFiscal.descripcion(item),
                    item.getCantidad(),
                    item.getPrecioFinal()));
        }
        return lineas;
    }

    private Map<String, Object> linea(String codigoInterno, String descripcion,
                                      int cantidad, BigDecimal precioFinalUnitario) {
        BigDecimal tasa = config.tasaIgv();
        BigDecimal precioUnitario = escala2(precioFinalUnitario);

        // Mas decimales aca reducen el descuadre; SUNAT los permite en el valor unitario.
        BigDecimal valorUnitario = precioFinalUnitario.divide(BigDecimal.ONE.add(tasa), 6, RoundingMode.HALF_UP);
        BigDecimal totalBaseIgv = escala2(valorUnitario.multiply(BigDecimal.valueOf(cantidad)));

        BigDecimal totalIgv;
        BigDecimal totalItem;
        if (config.isIgvPorResta()) {
            totalItem = escala2(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
            totalIgv = escala2(totalItem.subtract(totalBaseIgv));
        } else {
            totalIgv = escala2(totalBaseIgv.multiply(tasa));
            totalItem = escala2(totalBaseIgv.add(totalIgv));
        }

        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("codigo_interno", codigoInterno);
        linea.put("descripcion", descripcion);
        linea.put("unidad_de_medida", UNIDAD_MEDIDA);
        linea.put("cantidad", cantidad);
        linea.put("valor_unitario", valorUnitario);
        linea.put("codigo_tipo_precio", TIPO_PRECIO_VENTA);
        linea.put("precio_unitario", precioUnitario);
        linea.put("codigo_tipo_afectacion_igv", AFECTACION_GRAVADO);
        linea.put("total_base_igv", totalBaseIgv);
        linea.put("porcentaje_igv", config.getIgvPorcentaje());
        linea.put("total_igv", totalIgv);
        linea.put("total_impuestos", totalIgv);
        linea.put("total_valor_item", totalBaseIgv);
        linea.put("total_item", totalItem);
        return linea;
    }

    private Map<String, Object> totales(List<Map<String, Object>> lineas) {
        BigDecimal base = sumar(lineas, "total_base_igv");
        BigDecimal igv = sumar(lineas, "total_igv");
        BigDecimal total = sumar(lineas, "total_item");

        Map<String, Object> totales = new LinkedHashMap<>();
        totales.put("total_operaciones_gravadas", base);
        // Todo lo que vende el negocio es gravado, pero la API espera los tres montos.
        totales.put("total_operaciones_exoneradas", CERO);
        totales.put("total_operaciones_inafectas", CERO);
        totales.put("total_igv", igv);
        totales.put("total_impuestos", igv);
        totales.put("total_valor", base);
        totales.put("total_venta", total);
        return totales;
    }

    private BigDecimal sumar(List<Map<String, Object>> lineas, String campo) {
        return lineas.stream()
                .map(linea -> (BigDecimal) linea.get(campo))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal escala2(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }
}
