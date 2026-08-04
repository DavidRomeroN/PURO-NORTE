package com.anticucheria.exception;

import lombok.Getter;

@Getter
public class FactuSmartException extends RuntimeException {

    /**
     * Si el fallo puede resolverse solo (red caida, SUNAT lenta, emision en curso) la
     * boleta queda PENDIENTE y se puede reintentar. Si no (sin creditos, API key mala,
     * datos invalidos) queda en ERROR, porque necesita que alguien haga algo.
     */
    private final boolean reintentable;

    public FactuSmartException(String message, boolean reintentable) {
        super(message);
        this.reintentable = reintentable;
    }

    public FactuSmartException(String message, boolean reintentable, Throwable cause) {
        super(message, cause);
        this.reintentable = reintentable;
    }
}
