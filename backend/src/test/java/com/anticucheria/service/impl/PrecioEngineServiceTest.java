package com.anticucheria.service.impl;

import com.anticucheria.dto.request.SustitucionDTO;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.enums.TipoProducto;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Motor de precios")
class PrecioEngineServiceTest {

    private static final Long CORAZON = 1L;
    private static final Long CARNE = 2L;
    private static final Long SALCHICHA = 3L;
    private static final Long POLLO = 4L;
    private static final Long MOLLEJA = 5L;
    private static final Long CHORIZO = 6L;

    private static final Long MIXTO_SIMPLE = 1L;
    private static final Long MIXTO_ESPECIAL = 2L;

    private static final Long SIMPLE_SLOT_POLLO = 101L;
    private static final Long SIMPLE_SLOT_CARNE = 102L;
    private static final Long SIMPLE_SLOT_CORAZON = 103L;
    private static final Long SIMPLE_SLOT_SALCHICHA = 104L;

    private static final Long ESPECIAL_SLOT_POLLO = 201L;
    private static final Long ESPECIAL_SLOT_CHORIZO = 204L;

    @Mock
    private ProductoBaseRepository productoBaseRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private ComboSlotRepository comboSlotRepository;

    @InjectMocks
    private PrecioEngineServiceImpl precioEngine;

    @BeforeEach
    void configurarCatalogo() {
        ProductoBase corazon = producto(CORAZON, "Corazón", "6.00");
        ProductoBase carne = producto(CARNE, "Carne", "7.00");
        ProductoBase salchicha = producto(SALCHICHA, "Salchicha", "4.00");
        ProductoBase pollo = producto(POLLO, "Pollo", "6.00");
        ProductoBase molleja = producto(MOLLEJA, "Molleja", "6.00");
        ProductoBase chorizo = producto(CHORIZO, "Chorizo", "6.00");

        Combo mixtoSimple = combo(MIXTO_SIMPLE, "Mixto Simple", "17.00");
        Combo mixtoEspecial = combo(MIXTO_ESPECIAL, "Mixto Especial", "23.00");
        lenient().when(comboRepository.findById(MIXTO_SIMPLE)).thenReturn(Optional.of(mixtoSimple));
        lenient().when(comboRepository.findById(MIXTO_ESPECIAL)).thenReturn(Optional.of(mixtoEspecial));

        stubSlot(slot(SIMPLE_SLOT_POLLO, mixtoSimple, pollo, 1, false, true));
        stubSlot(slot(SIMPLE_SLOT_CARNE, mixtoSimple, carne, 2, false, true));
        stubSlot(slot(SIMPLE_SLOT_CORAZON, mixtoSimple, corazon, 3, false, true));
        stubSlot(slot(SIMPLE_SLOT_SALCHICHA, mixtoSimple, salchicha, 4, true, false));

        stubSlot(slot(ESPECIAL_SLOT_POLLO, mixtoEspecial, pollo, 1, false, true));
        stubSlot(slot(202L, mixtoEspecial, carne, 2, false, true));
        stubSlot(slot(203L, mixtoEspecial, corazon, 3, false, true));
        stubSlot(slot(ESPECIAL_SLOT_CHORIZO, mixtoEspecial, chorizo, 4, false, true));
        stubSlot(slot(205L, mixtoEspecial, salchicha, 5, true, false));
    }

    @Nested
    @DisplayName("Motor A — anticuchos sueltos")
    class MotorA {

        @Test
        @DisplayName("Pollo = 6.00")
        void simple() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(POLLO))).isEqualByComparingTo("6.00");
        }

        @Test
        @DisplayName("Pollo + Salchicha = 10.00")
        void dobleConSalchicha() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(POLLO, SALCHICHA))).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("Pollo + Pollo = 12.00 (componentes repetidos)")
        void dobleRepetido() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(POLLO, POLLO))).isEqualByComparingTo("12.00");
        }

        @Test
        @DisplayName("Pollo + Carne = 13.00")
        void doble() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(POLLO, CARNE))).isEqualByComparingTo("13.00");
        }

        @Test
        @DisplayName("Corazón x3 = 18.00")
        void tripleCorazon() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(CORAZON, CORAZON, CORAZON)))
                    .isEqualByComparingTo("18.00");
        }

        @Test
        @DisplayName("Carne x3 = 21.00")
        void tripleCarne() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(CARNE, CARNE, CARNE)))
                    .isEqualByComparingTo("21.00");
        }

        @Test
        @DisplayName("Carne + Molleja + Chorizo = 19.00")
        void tripleMixto() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(CARNE, MOLLEJA, CHORIZO)))
                    .isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("Sin componentes lanza excepción de negocio")
        void sinComponentes() {
            assertThatThrownBy(() -> precioEngine.calcularPrecioAnticucho(List.of()))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("El resultado siempre tiene escala 2")
        void escalaDos() {
            assertThat(precioEngine.calcularPrecioAnticucho(List.of(POLLO)).scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Motor B — Mixto Simple (base 17.00)")
    class MotorBSimple {

        @Test
        @DisplayName("Sin sustituciones = 17.00")
        void sinSustituciones() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of())).isEqualByComparingTo("17.00");
        }

        @Test
        @DisplayName("Lista nula de sustituciones = 17.00")
        void sustitucionesNulas() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, null)).isEqualByComparingTo("17.00");
        }

        @Test
        @DisplayName("Pollo → Carne = 18.00")
        void sustitucionMasCara() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of(sust(SIMPLE_SLOT_POLLO, CARNE))))
                    .isEqualByComparingTo("18.00");
        }

        @Test
        @DisplayName("Carne → Salchicha = 17.00 (la diferencia negativa no resta)")
        void sustitucionMasBarata() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of(sust(SIMPLE_SLOT_CARNE, SALCHICHA))))
                    .isEqualByComparingTo("17.00");
        }

        @Test
        @DisplayName("Carne → Pollo = 17.00 (la diferencia negativa no resta)")
        void sustitucionAlgoMasBarata() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of(sust(SIMPLE_SLOT_CARNE, POLLO))))
                    .isEqualByComparingTo("17.00");
        }

        @Test
        @DisplayName("Pollo → Molleja = 17.00 (mismo precio)")
        void sustitucionMismoPrecio() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of(sust(SIMPLE_SLOT_POLLO, MOLLEJA))))
                    .isEqualByComparingTo("17.00");
        }

        @Test
        @DisplayName("Pollo → Carne y Corazón → Carne = 19.00 (suma las diferencias positivas)")
        void sustitucionesMultiples() {
            List<SustitucionDTO> sustituciones = List.of(
                    sust(SIMPLE_SLOT_POLLO, CARNE),
                    sust(SIMPLE_SLOT_CORAZON, CARNE));
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, sustituciones)).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("Sustituir el slot de cortesía lanza excepción de negocio")
        void slotCortesiaNoSustituible() {
            List<SustitucionDTO> sustituciones = List.of(sust(SIMPLE_SLOT_SALCHICHA, CARNE));
            assertThatThrownBy(() -> precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, sustituciones))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("cortesía");
        }

        @Test
        @DisplayName("Un slot de otro combo lanza excepción de negocio")
        void slotDeOtroCombo() {
            List<SustitucionDTO> sustituciones = List.of(sust(ESPECIAL_SLOT_POLLO, CARNE));
            assertThatThrownBy(() -> precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, sustituciones))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no pertenece");
        }

        @Test
        @DisplayName("El resultado siempre tiene escala 2")
        void escalaDos() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_SIMPLE, List.of()).scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Motor B — Mixto Especial (base 23.00)")
    class MotorBEspecial {

        @Test
        @DisplayName("Sin sustituciones = 23.00")
        void sinSustituciones() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_ESPECIAL, List.of())).isEqualByComparingTo("23.00");
        }

        @Test
        @DisplayName("Pollo → Carne = 24.00")
        void sustitucionPollo() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_ESPECIAL, List.of(sust(ESPECIAL_SLOT_POLLO, CARNE))))
                    .isEqualByComparingTo("24.00");
        }

        @Test
        @DisplayName("Chorizo → Carne = 24.00")
        void sustitucionChorizo() {
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_ESPECIAL, List.of(sust(ESPECIAL_SLOT_CHORIZO, CARNE))))
                    .isEqualByComparingTo("24.00");
        }

        @Test
        @DisplayName("Pollo → Carne y Chorizo → Carne = 25.00")
        void sustitucionesMultiples() {
            List<SustitucionDTO> sustituciones = List.of(
                    sust(ESPECIAL_SLOT_POLLO, CARNE),
                    sust(ESPECIAL_SLOT_CHORIZO, CARNE));
            assertThat(precioEngine.calcularPrecioCombo(MIXTO_ESPECIAL, sustituciones)).isEqualByComparingTo("25.00");
        }
    }

    private SustitucionDTO sust(Long comboSlotId, Long productoBaseNuevoId) {
        return new SustitucionDTO(comboSlotId, productoBaseNuevoId);
    }

    private ProductoBase producto(Long id, String nombre, String precio) {
        ProductoBase producto = ProductoBase.builder()
                .id(id)
                .nombre(nombre)
                .tipo(TipoProducto.ANTICUCHO)
                .precioUnitario(new BigDecimal(precio))
                .activo(true)
                .build();
        lenient().when(productoBaseRepository.findById(id)).thenReturn(Optional.of(producto));
        return producto;
    }

    private Combo combo(Long id, String nombre, String precioBase) {
        return Combo.builder()
                .id(id)
                .nombre(nombre)
                .precioBase(new BigDecimal(precioBase))
                .activo(true)
                .build();
    }

    private ComboSlot slot(Long id, Combo combo, ProductoBase porDefecto, int orden,
                           boolean esCortesia, boolean esSustituible) {
        return ComboSlot.builder()
                .id(id)
                .combo(combo)
                .productoBaseDefault(porDefecto)
                .orden(orden)
                .esCortesia(esCortesia)
                .esSustituible(esSustituible)
                .build();
    }

    private void stubSlot(ComboSlot slot) {
        lenient().when(comboSlotRepository.findById(slot.getId())).thenReturn(Optional.of(slot));
    }
}
