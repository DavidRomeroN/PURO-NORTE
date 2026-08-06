package com.anticucheria.service.agrupacion;

import com.anticucheria.dto.response.ComponenteResponse;
import com.anticucheria.dto.response.PedidoItemResponse;
import com.anticucheria.model.enums.TipoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgrupadorItemsTest {

    private AgrupadorItems agrupador;

    @BeforeEach
    void setUp() {
        agrupador = new AgrupadorItems();
    }

    @Test
    void tresAnticuchosDeCarneIdenticos_unaLineaConCantidad3() {
        var lineas = agrupador.agrupar(List.of(
                anticucho(1L, 1, List.of(comp(3L, "Carne")), null, false),
                anticucho(2L, 1, List.of(comp(3L, "Carne")), null, false),
                anticucho(3L, 1, List.of(comp(3L, "Carne")), null, false)));

        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).getCantidad()).isEqualTo(3);
    }

    @Test
    void dosIdenticosYUnoConComentario_dosLineas() {
        var lineas = agrupador.agrupar(List.of(
                anticucho(1L, 1, List.of(comp(3L, "Carne")), null, false),
                anticucho(2L, 1, List.of(comp(3L, "Carne")), null, false),
                anticucho(3L, 1, List.of(comp(3L, "Carne")), "sin ají", false)));

        assertThat(lineas).hasSize(2);
        assertThat(lineas.get(0).getCantidad()).isEqualTo(2);
        assertThat(lineas.get(0).getObservacion()).isNull();
        assertThat(lineas.get(1).getCantidad()).isEqualTo(1);
        assertThat(lineas.get(1).getObservacion()).isEqualTo("sin ají");
    }

    @Test
    void polloCarneYCarnePollo_agrupanJuntos() {
        var lineas = agrupador.agrupar(List.of(
                anticucho(1L, 1, List.of(comp(4L, "Pollo"), comp(3L, "Carne")), null, false),
                anticucho(2L, 1, List.of(comp(3L, "Carne"), comp(4L, "Pollo")), null, false)));

        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).getCantidad()).isEqualTo(2);
    }

    @Test
    void mixtosConSustitucionesDistintas_noAgrupan() {
        var a = PedidoItemResponse.builder()
                .id(1L).tipoItem(TipoItem.COMBO).comboId(1L).comboNombre("Mixto")
                .cantidad(1).precioFinal(BigDecimal.TEN).paraLlevar(false)
                .componentes(List.of(
                        ComponenteResponse.builder().productoBaseId(3L).productoNombre("Carne")
                                .comboSlotId(10L).esSustitucion(true).build()))
                .build();
        var b = PedidoItemResponse.builder()
                .id(2L).tipoItem(TipoItem.COMBO).comboId(1L).comboNombre("Mixto")
                .cantidad(1).precioFinal(BigDecimal.TEN).paraLlevar(false)
                .componentes(List.of(
                        ComponenteResponse.builder().productoBaseId(4L).productoNombre("Pollo")
                                .comboSlotId(10L).esSustitucion(true).build()))
                .build();

        assertThat(agrupador.agrupar(List.of(a, b))).hasSize(2);
    }

    @Test
    void mismoItemUnoParaLlevarYOtroNo_noAgrupan() {
        var lineas = agrupador.agrupar(List.of(
                anticucho(1L, 1, List.of(comp(3L, "Carne")), null, true),
                anticucho(2L, 1, List.of(comp(3L, "Carne")), null, false)));

        assertThat(lineas).hasSize(2);
    }

    @Test
    void listaVacia_listaVacia() {
        assertThat(agrupador.agrupar(List.of())).isEmpty();
        assertThat(agrupador.agrupar(null)).isEmpty();
    }

    @Test
    void mixtoMuestraPalitosEnDescripcion() {
        var mixto = PedidoItemResponse.builder()
                .id(1L).tipoItem(TipoItem.COMBO).comboId(1L).comboNombre("Mixto Simple")
                .cantidad(1).precioFinal(BigDecimal.TEN).paraLlevar(false)
                .componentes(List.of(
                        ComponenteResponse.builder().productoBaseId(3L).productoNombre("Carne")
                                .comboSlotId(10L).esSustitucion(false).build(),
                        ComponenteResponse.builder().productoBaseId(1L).productoNombre("Corazón")
                                .comboSlotId(11L).esSustitucion(true).build()))
                .build();

        var lineas = agrupador.agrupar(List.of(mixto));
        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).getDescripcion()).isEqualTo("Mixto Simple · Carne + Corazón");
    }

    private static PedidoItemResponse anticucho(Long id, int cantidad, List<ComponenteResponse> comps,
                                                String obs, boolean llevar) {
        return PedidoItemResponse.builder()
                .id(id)
                .tipoItem(TipoItem.ANTICUCHO)
                .cantidad(cantidad)
                .precioFinal(BigDecimal.valueOf(7))
                .paraLlevar(llevar)
                .observaciones(obs)
                .componentes(comps)
                .build();
    }

    private static ComponenteResponse comp(Long id, String nombre) {
        return ComponenteResponse.builder()
                .productoBaseId(id)
                .productoNombre(nombre)
                .esSustitucion(false)
                .build();
    }
}
