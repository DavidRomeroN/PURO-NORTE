package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DespacharItemRequest {

    /**
     * true = el plato ya salió; false = se equivocó y vuelve a pendientes.
     * En la tablet es fácil tocar el plato equivocado, así que hay que poder deshacer.
     */
    @NotNull
    private Boolean despachado;
}
