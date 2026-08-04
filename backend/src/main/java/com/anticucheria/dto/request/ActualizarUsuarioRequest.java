package com.anticucheria.dto.request;

import com.anticucheria.model.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarUsuarioRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private Rol rol;
}
