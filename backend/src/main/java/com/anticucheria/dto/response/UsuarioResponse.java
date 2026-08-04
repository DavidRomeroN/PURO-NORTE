package com.anticucheria.dto.response;

import com.anticucheria.model.enums.Rol;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String usuario;
    private Rol rol;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
