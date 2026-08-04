package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarMesaRequest {

    @NotNull
    private Long mesaId;
}
