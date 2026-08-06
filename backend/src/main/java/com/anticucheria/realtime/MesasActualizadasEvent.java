package com.anticucheria.realtime;

/**
 * Señal de que el listado de mesas cambió. El payload se arma en AFTER_COMMIT
 * para no publicar un snapshot leído a medias dentro de la transacción.
 */
public record MesasActualizadasEvent() {
}
