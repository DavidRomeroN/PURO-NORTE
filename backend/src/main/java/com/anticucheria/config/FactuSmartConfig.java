package com.anticucheria.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.MathContext;

@Configuration
@ConfigurationProperties(prefix = "factusmart")
@Getter
@Setter
public class FactuSmartConfig {

    private String urlBase;

    private String apiKey;

    /**
     * Se envia explicitamente en cada peticion. La cuenta de integrador tiene mas de un
     * RUC registrado y la API solo lo asume sola cuando hay uno; si falta responde 422.
     */
    private String ruc;

    private int timeoutMs = 30000;

    private int igvPorcentaje = 18;

    /** Ver Parte 3.3 de la especificacion: solo activar tras probar en sandbox. */
    private boolean igvPorResta = false;

    /**
     * Permite fingir la emision cuando no hay API key, para desarrollar sin gastar
     * creditos. Solo lo activa el perfil dev; en produccion queda en false y entonces la
     * falta de API key impide arrancar (ver {@link ComprobacionDeFactuSmart}).
     */
    private boolean modoSimulado = false;

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Cierto solo cuando no hay con que emitir de verdad y esta permitido simular. */
    public boolean debeSimular() {
        return !estaConfigurado() && modoSimulado;
    }

    public BigDecimal tasaIgv() {
        return BigDecimal.valueOf(igvPorcentaje).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);
    }
}
