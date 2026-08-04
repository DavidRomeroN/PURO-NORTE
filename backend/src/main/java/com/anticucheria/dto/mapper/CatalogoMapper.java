package com.anticucheria.dto.mapper;

import com.anticucheria.dto.response.ComboResponse;
import com.anticucheria.dto.response.ComboSlotResponse;
import com.anticucheria.dto.response.MesaResponse;
import com.anticucheria.dto.response.ProductoResponse;
import com.anticucheria.dto.response.UsuarioResponse;
import com.anticucheria.model.Combo;
import com.anticucheria.model.ComboSlot;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.Usuario;

import java.util.List;

public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static UsuarioResponse toUsuarioResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .usuario(usuario.getUsuario())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .creadoEn(usuario.getCreadoEn())
                .build();
    }

    public static ProductoResponse toProductoResponse(ProductoBase producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .tipo(producto.getTipo())
                .precioUnitario(producto.getPrecioUnitario())
                .activo(producto.getActivo())
                .build();
    }

    public static ComboSlotResponse toComboSlotResponse(ComboSlot slot) {
        return ComboSlotResponse.builder()
                .id(slot.getId())
                .orden(slot.getOrden())
                .productoBaseDefault(toProductoResponse(slot.getProductoBaseDefault()))
                .esCortesia(slot.getEsCortesia())
                .esSustituible(slot.getEsSustituible())
                .build();
    }

    public static ComboResponse toComboResponse(Combo combo, List<ComboSlot> slots) {
        return ComboResponse.builder()
                .id(combo.getId())
                .nombre(combo.getNombre())
                .precioBase(combo.getPrecioBase())
                .activo(combo.getActivo())
                .slots(slots.stream().map(CatalogoMapper::toComboSlotResponse).toList())
                .build();
    }

    public static MesaResponse toMesaResponse(Mesa mesa) {
        return MesaResponse.builder()
                .id(mesa.getId())
                .numero(mesa.getNumero())
                .estado(mesa.getEstado())
                .build();
    }
}
