package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private UsuarioResponse usuario;
}
