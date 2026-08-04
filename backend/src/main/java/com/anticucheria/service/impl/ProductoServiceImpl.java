package com.anticucheria.service.impl;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.request.ProductoRequest;
import com.anticucheria.dto.response.ProductoResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.enums.TipoProducto;
import com.anticucheria.repository.ComboSlotRepository;
import com.anticucheria.repository.ProductoBaseRepository;
import com.anticucheria.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoBaseRepository productoBaseRepository;
    private final ComboSlotRepository comboSlotRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(TipoProducto tipo, boolean incluirInactivos) {
        List<ProductoBase> productos;
        if (tipo != null) {
            productos = incluirInactivos
                    ? productoBaseRepository.findByTipoOrderByNombreAsc(tipo)
                    : productoBaseRepository.findByTipoAndActivoTrueOrderByNombreAsc(tipo);
        } else {
            productos = incluirInactivos
                    ? productoBaseRepository.findAllByOrderByNombreAsc()
                    : productoBaseRepository.findByActivoTrueOrderByNombreAsc();
        }
        return productos.stream().map(CatalogoMapper::toProductoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return CatalogoMapper.toProductoResponse(buscar(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        ProductoBase producto = ProductoBase.builder()
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .precioUnitario(request.getPrecioUnitario())
                .activo(true)
                .build();
        return CatalogoMapper.toProductoResponse(productoBaseRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        ProductoBase producto = buscar(id);
        producto.setNombre(request.getNombre());
        producto.setTipo(request.getTipo());
        producto.setPrecioUnitario(request.getPrecioUnitario());
        return CatalogoMapper.toProductoResponse(productoBaseRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse cambiarEstado(Long id, boolean activo) {
        ProductoBase producto = buscar(id);

        if (!activo) {
            List<String> combos = comboSlotRepository.findByProductoBaseDefaultIdAndComboActivoTrue(id).stream()
                    .map(slot -> slot.getCombo().getNombre())
                    .distinct()
                    .toList();
            if (!combos.isEmpty()) {
                throw new ReglaNegocioException(mensajeProductoEnCombo(producto.getNombre(), combos));
            }
        }

        producto.setActivo(activo);
        return CatalogoMapper.toProductoResponse(productoBaseRepository.save(producto));
    }

    /** El mensaje llega tal cual a la dueña, así que se redacta como se lo diría una persona. */
    private String mensajeProductoEnCombo(String producto, List<String> combos) {
        if (combos.size() == 1) {
            return "No se puede ocultar " + producto + " porque forma parte del " + combos.get(0)
                    + ". Cambia primero el contenido del combo.";
        }
        String lista = String.join(", ", combos.subList(0, combos.size() - 1))
                + " y " + combos.get(combos.size() - 1);
        return "No se puede ocultar " + producto + " porque forma parte de " + lista
                + ". Cambia primero el contenido de esos combos.";
    }

    private ProductoBase buscar(Long id) {
        return productoBaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }
}
