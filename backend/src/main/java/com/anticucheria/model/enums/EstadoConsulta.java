package com.anticucheria.model.enums;

/**
 * Resultado de verificar un documento. No encontrarlo y no poder verificarlo son cosas
 * distintas: lo primero apunta a un numero mal tecleado, lo segundo a que el servicio no
 * respondio. Ninguno de los dos impide emitir la boleta.
 */
public enum EstadoConsulta {
    ENCONTRADO,
    NO_ENCONTRADO,
    NO_VERIFICADO
}
