package com.anticucheria.controller;

import com.anticucheria.dto.request.CambiarEstadoRequest;
import com.anticucheria.dto.request.ProductoRequest;
import com.anticucheria.dto.response.ProductoResponse;
import com.anticucheria.model.enums.TipoProducto;
import com.anticucheria.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("!#incluirInactivos or hasRole('ADMIN')")
    public List<ProductoResponse> listar(
            @RequestParam(required = false) TipoProducto tipo,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return productoService.listar(tipo, incluirInactivos);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody CambiarEstadoRequest request) {
        return productoService.cambiarEstado(id, request.getActivo());
    }
}
