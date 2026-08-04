package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditosResponse {

    private Integer creditosDisponibles;

    /** Falso cuando la emision quedo suspendida por un error de configuracion del RUC. */
    private boolean emisionActiva;

    /** Por que se suspendio, cuando aplica. */
    private String motivoSuspension;
}
