package com.anticucheria.service;

import com.anticucheria.dto.request.ProductoRequest;
import com.anticucheria.dto.response.ProductoResponse;
import com.anticucheria.model.enums.TipoProducto;

import java.util.List;

public interface ProductoService {

    List<ProductoResponse> listar(TipoProducto tipo, boolean incluirInactivos);

    ProductoResponse obtener(Long id);

    ProductoResponse crear(ProductoRequest request);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    ProductoResponse cambiarEstado(Long id, boolean activo);
}
