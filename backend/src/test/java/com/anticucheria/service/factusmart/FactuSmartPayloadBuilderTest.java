package com.anticucheria.service.factusmart;

import com.anticucheria.config.FactuSmartConfig;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.Combo;
import com.anticucheria.model.PedidoItem;
import com.anticucheria.model.PedidoItemComponente;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.enums.TipoBoleta;
import com.anticucheria.model.enums.TipoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Estos tests construyen el JSON y lo revisan en memoria. Ninguno sale a la red: cada
 * emision real cuesta un credito, tambien en sandbox, y solo hay cien.
 */
class FactuSmartPayloadBuilderTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private FactuSmartConfig config;
    private FactuSmartPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        config = new FactuSmartConfig();
        config.setRuc("10013193482");
        config.setIgvPorcentaje(18);
        config.setIgvPorResta(false);

        // 2026-08-04 19:45:00 en Lima.
        Clock reloj = Clock.fixed(Instant.parse("2026-08-05T00:45:00Z"), LIMA);
        builder = new FactuSmartPayloadBuilder(config, reloj);
    }

    @Test
    @DisplayName("boleta por consumo: un solo item con el total del pedido")
    void boletaPorConsumo() {
        Map<String, Object> payload = builder.construir(
                boleta(TipoBoleta.CONSUMO, "28.00", null), List.of());

        assertThat(payload.get("codigo_tipo_documento")).isEqualTo("03");
        assertThat(payload.get("codigo_tipo_operacion")).isEqualTo("0101");
        assertThat(payload.get("codigo_tipo_moneda")).isEqualTo("PEN");
        // La serie de una boleta la pone FactuSmart.
        assertThat(payload).doesNotContainKey("serie_documento");
        assertThat(payload.get("numero_documento")).isEqualTo("#");
        assertThat(payload.get("ruc")).isEqualTo("10013193482");

        List<Map<String, Object>> items = items(payload);
        assertThat(items).hasSize(1);

        Map<String, Object> item = items.get(0);
        assertThat(item.get("codigo_interno")).isEqualTo("CONSUMO");
        assertThat(item.get("descripcion")).isEqualTo("Consumo");
        assertThat(item.get("unidad_de_medida")).isEqualTo("NIU");
        assertThat(item.get("cantidad")).isEqualTo(1);
        assertThat(item.get("codigo_tipo_afectacion_igv")).isEqualTo("10");
        assertThat(item.get("valor_unitario")).isEqualTo(new BigDecimal("23.728814"));
        assertThat(item.get("precio_unitario")).isEqualTo(new BigDecimal("28.00"));
        assertThat(item.get("total_base_igv")).isEqualTo(new BigDecimal("23.73"));
        assertThat(item.get("total_igv")).isEqualTo(new BigDecimal("4.27"));
        assertThat(item.get("total_item")).isEqualTo(new BigDecimal("28.00"));

        Map<String, Object> totales = totales(payload);
        assertThat(totales.get("total_operaciones_gravadas")).isEqualTo(new BigDecimal("23.73"));
        assertThat(totales.get("total_igv")).isEqualTo(new BigDecimal("4.27"));
        assertThat(totales.get("total_venta")).isEqualTo(new BigDecimal("28.00"));
    }

    @Test
    @DisplayName("el bloque totales usa los nombres exactos de la documentacion")
    void nombresDelBloqueTotales() {
        Map<String, Object> totales = totales(builder.construir(
                boleta(TipoBoleta.CONSUMO, "118.00", null), List.of()));

        // Un nombre distinto hace que la API rechace la boleta entera, asi que se fija la
        // lista completa y en orden: si alguien agrega o renombra un campo, este test cae.
        assertThat(totales).containsExactly(
                entry("total_operaciones_gravadas", new BigDecimal("100.00")),
                entry("total_operaciones_exoneradas", new BigDecimal("0.00")),
                entry("total_operaciones_inafectas", new BigDecimal("0.00")),
                entry("total_igv", new BigDecimal("18.00")),
                entry("total_impuestos", new BigDecimal("18.00")),
                entry("total_valor", new BigDecimal("100.00")),
                entry("total_venta", new BigDecimal("118.00")));
    }

    @Test
    @DisplayName("fecha y hora salen en zona de Peru, no en UTC")
    void fechaEnZonaDePeru() {
        Map<String, Object> payload = builder.construir(
                boleta(TipoBoleta.CONSUMO, "28.00", null), List.of());

        // En UTC ya seria el dia 5; en Lima siguen siendo las 19:45 del 4.
        assertThat(payload.get("fecha_de_emision")).isEqualTo("2026-08-04");
        assertThat(payload.get("hora_de_emision")).isEqualTo("19:45:00");
    }

    @Test
    @DisplayName("boleta detallada: un item por linea del pedido y totales que suman")
    void boletaDetalladaConTresItems() {
        PedidoItem mixto = anticucho("17.00", 1, producto(3L, "Carne"), producto(4L, "Pollo"));
        PedidoItem carne = anticucho("7.00", 2, producto(3L, "Carne"));
        PedidoItem gaseosa = simple(TipoItem.BEBIDA, "5.00", 1, producto(9L, "Inca Kola"));

        Map<String, Object> payload = builder.construir(
                boleta(TipoBoleta.DETALLADO, "36.00", null), List.of(mixto, carne, gaseosa));

        List<Map<String, Object>> items = items(payload);
        assertThat(items).hasSize(3);
        assertThat(items.get(0).get("descripcion")).isEqualTo("Carne + Pollo");
        assertThat(items.get(1).get("cantidad")).isEqualTo(2);
        assertThat(items.get(2).get("codigo_interno")).isEqualTo("PROD-9");

        Map<String, Object> totales = totales(payload);
        assertThat(totales.get("total_operaciones_gravadas")).isEqualTo(new BigDecimal("30.51"));
        assertThat(totales.get("total_igv")).isEqualTo(new BigDecimal("5.48"));
        // Se cobraron S/36.00 y la boleta suma 35.99: el centimo lo pierde la linea de
        // 7.00 x 2 al redondear. Es el mismo descuadre del caso de S/6.00.
        assertThat(totales.get("total_venta")).isEqualTo(new BigDecimal("35.99"));
    }

    @Nested
    @DisplayName("el item de S/6.00, que es el que descuadra")
    class ItemDeSeisSoles {

        @Test
        @DisplayName("con el algoritmo de referencia el total reconstruido da 5.99")
        void conAlgoritmoDeReferencia() {
            Map<String, Object> item = primerItem("6.00");

            assertThat(item.get("valor_unitario")).isEqualTo(new BigDecimal("5.084746"));
            assertThat(item.get("total_base_igv")).isEqualTo(new BigDecimal("5.08"));
            assertThat(item.get("total_igv")).isEqualTo(new BigDecimal("0.91"));
            // Un centimo menos de lo cobrado. Documentado a proposito: es el comportamiento
            // del algoritmo que publica el proveedor y es lo que su validacion espera.
            assertThat(item.get("total_item")).isEqualTo(new BigDecimal("5.99"));
            // El precio unitario si refleja lo que pago el cliente y es lo que sale en el PDF.
            assertThat(item.get("precio_unitario")).isEqualTo(new BigDecimal("6.00"));
        }

        @Test
        @DisplayName("con igv-por-resta el total cuadra exacto con lo cobrado")
        void conIgvPorResta() {
            config.setIgvPorResta(true);
            Map<String, Object> item = primerItem("6.00");

            assertThat(item.get("total_base_igv")).isEqualTo(new BigDecimal("5.08"));
            assertThat(item.get("total_igv")).isEqualTo(new BigDecimal("0.92"));
            assertThat(item.get("total_item")).isEqualTo(new BigDecimal("6.00"));
        }

        private Map<String, Object> primerItem(String monto) {
            return items(builder.construir(boleta(TipoBoleta.CONSUMO, monto, null), List.of())).get(0);
        }
    }

    @Test
    @DisplayName("cliente con DNI: no se manda el nombre, lo completa RENIEC")
    void clienteConDni() {
        Map<String, Object> cliente = cliente(builder.construir(
                boleta(TipoBoleta.CONSUMO, "28.00", "12345678"), List.of()));

        assertThat(cliente.get("codigo_tipo_documento_identidad")).isEqualTo("1");
        assertThat(cliente.get("numero_documento")).isEqualTo("12345678");
        assertThat(cliente).doesNotContainKey("apellidos_y_nombres_o_razon_social");
    }

    @Test
    @DisplayName("cliente anonimo: se manda CLIENTE VARIOS")
    void clienteAnonimo() {
        Map<String, Object> cliente = cliente(builder.construir(
                boleta(TipoBoleta.CONSUMO, "28.00", null), List.of()));

        assertThat(cliente.get("codigo_tipo_documento_identidad")).isEqualTo("0");
        assertThat(cliente.get("numero_documento")).isEqualTo("00000000");
        assertThat(cliente.get("apellidos_y_nombres_o_razon_social")).isEqualTo("CLIENTE VARIOS");
    }

    @Test
    @DisplayName("el codigo de un anticucho doble no depende del orden de sus componentes")
    void codigoInternoEstableSinImportarElOrden() {
        PedidoItem carnePollo = anticucho("17.00", 1, producto(3L, "Carne"), producto(4L, "Pollo"));
        PedidoItem polloCarne = anticucho("17.00", 1, producto(4L, "Pollo"), producto(3L, "Carne"));

        assertThat(ItemFiscal.codigoInterno(carnePollo))
                .isEqualTo(ItemFiscal.codigoInterno(polloCarne))
                .isEqualTo("ANT-3-4");
    }

    @Test
    @DisplayName("un combo usa el id del combo como codigo de catalogo")
    void codigoInternoDeCombo() {
        PedidoItem item = simple(TipoItem.COMBO, "22.00", 1, producto(3L, "Carne"));
        item.setCombo(Combo.builder().id(2L).nombre("Mixto Simple").build());

        assertThat(ItemFiscal.codigoInterno(item)).isEqualTo("COMBO-2");
        assertThat(ItemFiscal.descripcion(item)).isEqualTo("Mixto Simple");
    }

    @Test
    @DisplayName("los montos van con dos decimales y el valor unitario con seis")
    void escalasDeLosMontos() {
        Map<String, Object> payload = builder.construir(
                boleta(TipoBoleta.CONSUMO, "17.00", null), List.of());
        Map<String, Object> item = items(payload).get(0);

        List.of("precio_unitario", "total_base_igv", "total_igv", "total_impuestos",
                        "total_valor_item", "total_item")
                .forEach(campo -> assertThat((BigDecimal) item.get(campo))
                        .describedAs(campo)
                        .hasScaleOf(2));

        totales(payload).values()
                .forEach(monto -> assertThat((BigDecimal) monto).hasScaleOf(2));

        // Mas precision aca es lo que reduce el descuadre de centimos, y SUNAT la permite.
        assertThat((BigDecimal) item.get("valor_unitario")).hasScaleOf(6);
    }

    // --- Ayudas ------------------------------------------------------------------------

    private Boleta boleta(TipoBoleta tipo, String montoTotal, String dni) {
        return Boleta.builder()
                .id(1L)
                .tipo(tipo)
                .montoTotal(new BigDecimal(montoTotal))
                .clienteDocumento(dni)
                .build();
    }

    private ProductoBase producto(Long id, String nombre) {
        return ProductoBase.builder().id(id).nombre(nombre).build();
    }

    private PedidoItem anticucho(String precio, int cantidad, ProductoBase... productos) {
        return simple(TipoItem.ANTICUCHO, precio, cantidad, productos);
    }

    private PedidoItem simple(TipoItem tipo, String precio, int cantidad, ProductoBase... productos) {
        PedidoItem item = PedidoItem.builder()
                .tipoItem(tipo)
                .cantidad(cantidad)
                .precioFinal(new BigDecimal(precio))
                .precioCalculado(new BigDecimal(precio))
                .componentes(new ArrayList<>())
                .build();

        for (ProductoBase producto : productos) {
            item.getComponentes().add(PedidoItemComponente.builder()
                    .productoBase(producto)
                    .precioUnitarioSnapshot(producto.getPrecioUnitario())
                    .build());
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> payload) {
        return (List<Map<String, Object>>) payload.get("items");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> totales(Map<String, Object> payload) {
        return (Map<String, Object>) payload.get("totales");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cliente(Map<String, Object> payload) {
        return (Map<String, Object>) payload.get("datos_del_cliente_o_receptor");
    }
}
