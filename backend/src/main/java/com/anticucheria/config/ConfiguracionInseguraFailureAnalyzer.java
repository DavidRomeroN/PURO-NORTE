package com.anticucheria.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Saca el fallo de configuracion a la vista, en el bloque legible de Spring Boot, en vez
 * de dejarlo enterrado en la traza de un BeanCreationException que nadie lee.
 */
public class ConfiguracionInseguraFailureAnalyzer
        extends AbstractFailureAnalyzer<ConfiguracionInseguraException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfiguracionInseguraException cause) {
        return new FailureAnalysis(cause.getMessage(), cause.getAccion(), cause);
    }
}
