package com.anticucheria.controller;

import com.anticucheria.dto.request.CambiarEstadoRequest;
import com.anticucheria.dto.request.ComboRequest;
import com.anticucheria.dto.request.ComboSlotRequest;
import com.anticucheria.dto.response.ComboResponse;
import com.anticucheria.service.ComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    @PreAuthorize("!#incluirInactivos or hasRole('ADMIN')")
    public List<ComboResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return comboService.listar(incluirInactivos);
    }

    @GetMapping("/{id}")
    public ComboResponse obtener(@PathVariable Long id) {
        return comboService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ComboResponse crear(@Valid @RequestBody ComboRequest request) {
        return comboService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComboResponse actualizar(@PathVariable Long id, @Valid @RequestBody ComboRequest request) {
        return comboService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ComboResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return comboService.cambiarEstado(id, request.getActivo());
    }

    @PostMapping("/{comboId}/slots")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ComboResponse agregarSlot(@PathVariable Long comboId, @Valid @RequestBody ComboSlotRequest request) {
        return comboService.agregarSlot(comboId, request);
    }

    @PutMapping("/{comboId}/slots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComboResponse actualizarSlot(@PathVariable Long comboId, @PathVariable Long id,
                                        @Valid @RequestBody ComboSlotRequest request) {
        return comboService.actualizarSlot(comboId, id, request);
    }

    @DeleteMapping("/{comboId}/slots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComboResponse eliminarSlot(@PathVariable Long comboId, @PathVariable Long id) {
        return comboService.eliminarSlot(comboId, id);
    }
}
