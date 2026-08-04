package com.anticucheria.dto.request;

import com.anticucheria.model.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearUsuarioRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(min = 3, max = 50)
    private String usuario;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotNull
    private Rol rol;
}
