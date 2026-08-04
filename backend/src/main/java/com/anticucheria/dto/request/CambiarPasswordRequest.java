package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
