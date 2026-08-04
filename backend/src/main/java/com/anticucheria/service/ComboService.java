package com.anticucheria.service;

import com.anticucheria.dto.request.ComboRequest;
import com.anticucheria.dto.request.ComboSlotRequest;
import com.anticucheria.dto.response.ComboResponse;

import java.util.List;

public interface ComboService {

    List<ComboResponse> listar(boolean incluirInactivos);

    ComboResponse obtener(Long id);

    ComboResponse crear(ComboRequest request);

    ComboResponse actualizar(Long id, ComboRequest request);

    ComboResponse cambiarEstado(Long id, boolean activo);

    ComboResponse agregarSlot(Long comboId, ComboSlotRequest request);

    ComboResponse actualizarSlot(Long comboId, Long slotId, ComboSlotRequest request);

    ComboResponse eliminarSlot(Long comboId, Long slotId);
}
