package com.anticucheria.config;

import lombok.Getter;

/**
 * Se lanza durante el arranque cuando la configuracion haria funcionar el sistema de una
 * forma insegura y es mejor no abrir que abrir mal.
 *
 * Trae ya redactados el que pasa y el que hacer, porque quien detecta el problema es quien
 * sabe explicarlo; {@link ConfiguracionInseguraFailureAnalyzer} los imprime tal cual.
 */
@Getter
public class ConfiguracionInseguraException extends RuntimeException {

    private final String accion;

    public ConfiguracionInseguraException(String descripcion, String accion) {
        super(descripcion);
        this.accion = accion;
    }
}
