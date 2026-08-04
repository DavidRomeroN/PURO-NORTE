package com.anticucheria.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Impide que la aplicacion arranque si no tiene forma de emitir comprobantes.
 *
 * El modo simulado existe para poder desarrollar sin gastar los creditos de FactuSmart,
 * pero en produccion es un riesgo real: la caja cobraria normalmente y cada venta
 * quedaria sin boleta ante SUNAT, sin que nadie lo note hasta la fiscalizacion. Por eso
 * solo lo habilita el perfil dev, y sin API key ni modo simulado el arranque falla.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComprobacionDeFactuSmart implements InitializingBean {

    private final FactuSmartConfig config;

    @Override
    public void afterPropertiesSet() {
        if (config.estaConfigurado()) {
            if (config.getRuc() == null || config.getRuc().isBlank()) {
                throw new ConfiguracionInseguraException(
                        "Hay API key de FactuSmart pero falta el RUC del emisor.",
                        "Define FACTUSMART_RUC con el RUC de la empresa y vuelve a arrancar.");
            }
            return;
        }
        if (!config.isModoSimulado()) {
            throw new ConfiguracionInseguraException(
                    "No hay forma de emitir comprobantes electronicos: factusmart.api-key esta "
                            + "vacia y el modo simulado no esta permitido en este perfil.",
                    """
                    Define la variable de entorno FACTUSMART_API_KEY con la clave de la cuenta \
                    y vuelve a arrancar.

                    El modo simulado, que finge la emision para desarrollar sin gastar \
                    creditos, solo existe en el perfil dev. En produccion no se permite: \
                    cobrar sin emitir la boleta real deja la venta sin comprobante ante SUNAT.

                    Para trabajar en local sin API key: SPRING_PROFILES_ACTIVE=dev""");
        }

        log.warn("""
                ================================================================
                MODO SIMULADO: no hay API key de FactuSmart.
                Las boletas se guardan con simulada=true y NO tienen validez
                fiscal. Nunca arranques asi en el local.
                ================================================================""");
    }
}
