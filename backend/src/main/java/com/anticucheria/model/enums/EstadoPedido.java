package com.anticucheria.model.enums;

public enum EstadoPedido {
    ABIERTO,
    CERRADO,
    PAGADO,
    /** La cuenta se descarto sin cobrar. Libera las mesas y no cuenta como venta. */
    ANULADO
}
