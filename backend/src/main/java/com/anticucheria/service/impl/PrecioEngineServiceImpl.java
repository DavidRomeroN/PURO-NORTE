package com.anticucheria.service.impl;

import com.anticucheria.dto.request.SustitucionDTO;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.service.PrecioEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrecioEngineServiceImpl implements PrecioEngineService {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final ProductoBaseRepository productoBaseRepository;
    private final ComboRepository comboRepository;
    private final ComboSlotRepository comboSlotRepository;

    /** Motor A: suma pura de los componentes elegidos. Admite componentes repetidos y N componentes. */
    @Override
    public BigDecimal calcularPrecioAnticucho(List<Long> productoBaseIds) {
        if (productoBaseIds == null || productoBaseIds.isEmpty()) {
            throw new ReglaNegocioException("Un anticucho debe tener al menos un componente");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Long productoBaseId : productoBaseIds) {
            total = total.add(obtenerPrecioProducto(productoBaseId));
        }
        return escalar(total);
    }

    /** Motor B: precio_base del combo más la suma de las diferencias positivas de cada sustitución. */
    @Override
    public BigDecimal calcularPrecioCombo(Long comboId, List<SustitucionDTO> sustituciones) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado: " + comboId));

        BigDecimal precioFinal = combo.getPrecioBase();

        if (sustituciones != null) {
            for (SustitucionDTO sustitucion : sustituciones) {
                ComboSlot slot = comboSlotRepository.findById(sustitucion.getComboSlotId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Slot de combo no encontrado: " + sustitucion.getComboSlotId()));

                if (Boolean.TRUE.equals(slot.getEsCortesia()) || !Boolean.TRUE.equals(slot.getEsSustituible())) {
                    throw new ReglaNegocioException("El slot de cortesía no es sustituible");
                }

                if (!slot.getCombo().getId().equals(comboId)) {
                    throw new ReglaNegocioException("El slot no pertenece a este combo");
                }

                BigDecimal precioOriginal = slot.getProductoBaseDefault().getPrecioUnitario();
                BigDecimal precioNuevo = obtenerPrecioProducto(sustitucion.getProductoBaseNuevoId());

                BigDecimal diferencia = precioNuevo.subtract(precioOriginal).max(BigDecimal.ZERO);
                precioFinal = precioFinal.add(diferencia);
            }
        }

        return escalar(precioFinal);
    }

    @Override
    public BigDecimal calcularPrecioItemSimple(Long productoBaseId) {
        return escalar(obtenerPrecioProducto(productoBaseId));
    }

    private BigDecimal obtenerPrecioProducto(Long productoBaseId) {
        ProductoBase producto = productoBaseRepository.findById(productoBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoBaseId));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("Producto inactivo: " + producto.getNombre());
        }
        return producto.getPrecioUnitario();
    }

    private BigDecimal escalar(BigDecimal monto) {
        return monto.setScale(ESCALA, REDONDEO);
    }
}
