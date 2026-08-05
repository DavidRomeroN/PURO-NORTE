package com.anticucheria.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnviarCorreoBoletaRequest {

    @NotBlank
    @Email
    private String correo;
}
