package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnularPedidoRequest {

    /**
     * Obligatorio, al reves que el motivo de editar un precio. Anular es la forma mas
     * facil de que una mesa que consumio no pase por caja, y el motivo es lo unico que
     * despues permite revisar si el descarte tenia sentido.
     */
    @NotBlank(message = "hay que indicar por qué se anula la cuenta")
    @Size(max = 300)
    private String motivo;
}
