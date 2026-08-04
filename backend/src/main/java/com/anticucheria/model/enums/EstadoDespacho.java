package com.anticucheria.model.enums;

/**
 * Si el plato ya salió de la parrilla. El parrillero se guía por esto: lo pendiente
 * sigue en la lista hasta que lo marca, y lo despachado queda como historial del pedido.
 */
public enum EstadoDespacho {
    PENDIENTE,
    DESPACHADO
}
