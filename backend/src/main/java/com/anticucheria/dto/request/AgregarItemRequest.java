package com.anticucheria.dto.request;

import com.anticucheria.model.enums.TipoItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AgregarItemRequest {

    @NotNull
    private TipoItem tipoItem;

    /** Requerido solo si tipoItem = COMBO. */
    private Long comboId;

    @NotNull
    @Positive
    private Integer cantidad = 1;

    /** Ids de productos_base elegidos (anticucho, bebida o extra). */
    private List<Long> componentes = new ArrayList<>();

    /** Sustituciones de slots, solo para combos. */
    @Valid
    private List<SustitucionDTO> sustituciones = new ArrayList<>();

    private Boolean paraLlevar = false;

    @Size(max = 255)
    private String observaciones;
}
