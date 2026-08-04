package com.anticucheria.service.impl;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.request.ComboRequest;
import com.anticucheria.dto.request.ComboSlotRequest;
import com.anticucheria.dto.response.ComboResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.repository.ComboRepository;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepository;
    private final ComboSlotRepository comboSlotRepository;
    private final ProductoBaseRepository productoBaseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComboResponse> listar(boolean incluirInactivos) {
        List<Combo> combos = incluirInactivos
                ? comboRepository.findAllByOrderByNombreAsc()
                : comboRepository.findByActivoTrueOrderByNombreAsc();
        return combos.stream().map(this::conSlots).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComboResponse obtener(Long id) {
        return conSlots(buscarCombo(id));
    }

    @Override
    @Transactional
    public ComboResponse crear(ComboRequest request) {
        Combo combo = Combo.builder()
                .nombre(request.getNombre())
                .precioBase(request.getPrecioBase())
                .activo(true)
                .build();
        return conSlots(comboRepository.save(combo));
    }

    @Override
    @Transactional
    public ComboResponse actualizar(Long id, ComboRequest request) {
        Combo combo = buscarCombo(id);
        combo.setNombre(request.getNombre());
        combo.setPrecioBase(request.getPrecioBase());
        return conSlots(comboRepository.save(combo));
    }

    @Override
    @Transactional
    public ComboResponse cambiarEstado(Long id, boolean activo) {
        Combo combo = buscarCombo(id);
        combo.setActivo(activo);
        return conSlots(comboRepository.save(combo));
    }

    @Override
    @Transactional
    public ComboResponse agregarSlot(Long comboId, ComboSlotRequest request) {
        Combo combo = buscarCombo(comboId);
        ComboSlot slot = ComboSlot.builder()
                .combo(combo)
                .productoBaseDefault(buscarProducto(request.getProductoBaseDefaultId()))
                .orden(request.getOrden())
                .esCortesia(request.getEsCortesia())
                .esSustituible(request.getEsSustituible())
                .build();
        comboSlotRepository.save(slot);
        return conSlots(combo);
    }

    @Override
    @Transactional
    public ComboResponse actualizarSlot(Long comboId, Long slotId, ComboSlotRequest request) {
        Combo combo = buscarCombo(comboId);
        ComboSlot slot = buscarSlot(comboId, slotId);

        slot.setProductoBaseDefault(buscarProducto(request.getProductoBaseDefaultId()));
        slot.setOrden(request.getOrden());
        slot.setEsCortesia(request.getEsCortesia());
        slot.setEsSustituible(request.getEsSustituible());
        comboSlotRepository.save(slot);

        return conSlots(combo);
    }

    @Override
    @Transactional
    public ComboResponse eliminarSlot(Long comboId, Long slotId) {
        Combo combo = buscarCombo(comboId);
        ComboSlot slot = buscarSlot(comboId, slotId);

        if (comboSlotRepository.countByComboId(comboId) <= 1) {
            throw new ReglaNegocioException("El combo debe conservar al menos un slot");
        }

        comboSlotRepository.delete(slot);
        return conSlots(combo);
    }

    private ComboResponse conSlots(Combo combo) {
        return CatalogoMapper.toComboResponse(combo, comboSlotRepository.findByComboIdOrderByOrdenAsc(combo.getId()));
    }

    private Combo buscarCombo(Long id) {
        return comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado: " + id));
    }

    private ComboSlot buscarSlot(Long comboId, Long slotId) {
        ComboSlot slot = comboSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado: " + slotId));
        if (!slot.getCombo().getId().equals(comboId)) {
            throw new ReglaNegocioException("El slot no pertenece a este combo");
        }
        return slot;
    }

    private ProductoBase buscarProducto(Long id) {
        return productoBaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }
}
