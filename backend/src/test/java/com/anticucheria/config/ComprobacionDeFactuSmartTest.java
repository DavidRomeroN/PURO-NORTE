package com.anticucheria.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lo que se protege aca es que nadie pueda abrir la caja en el local sin poder emitir
 * boletas. Con el modo simulado la venta se cobraria igual y no quedaria comprobante
 * ante SUNAT, y nadie se enteraria hasta la fiscalizacion.
 */
class ComprobacionDeFactuSmartTest {

    @Test
    @DisplayName("sin API key y sin modo simulado, la aplicacion no arranca")
    void sinApiKeyNiModoSimuladoFalla() {
        assertThatThrownBy(() -> comprobar(null, false))
                .isInstanceOf(ConfiguracionInseguraException.class)
                .hasMessageContaining("api-key");

        // Una key en blanco es el caso real: la variable de entorno existe pero vacia.
        assertThatThrownBy(() -> comprobar("   ", false))
                .isInstanceOf(ConfiguracionInseguraException.class);
    }

    @Test
    @DisplayName("sin API key pero en modo simulado, arranca para poder desarrollar")
    void modoSimuladoArranca() {
        assertThatCode(() -> comprobar("", true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("con API key y RUC arranca en cualquier perfil")
    void conApiKeyArranca() {
        assertThatCode(() -> comprobar("clave-real", "20123456789", false)).doesNotThrowAnyException();
        assertThatCode(() -> comprobar("clave-real", "20123456789", true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("con API key pero sin RUC no arranca")
    void conApiKeySinRucFalla() {
        assertThatThrownBy(() -> comprobar("clave-real", "", false))
                .isInstanceOf(ConfiguracionInseguraException.class)
                .hasMessageContaining("RUC");
    }

    @Test
    @DisplayName("si hay API key no se simula, aunque el modo simulado este activo")
    void laApiKeyGanaAlModoSimulado() {
        assertThat(config("clave-real", "20123456789", true).debeSimular()).isFalse();
        assertThat(config(null, null, true).debeSimular()).isTrue();
        assertThat(config(null, null, false).debeSimular()).isFalse();
    }

    private void comprobar(String apiKey, boolean modoSimulado) {
        comprobar(apiKey, null, modoSimulado);
    }

    private void comprobar(String apiKey, String ruc, boolean modoSimulado) {
        new ComprobacionDeFactuSmart(config(apiKey, ruc, modoSimulado)).afterPropertiesSet();
    }

    private FactuSmartConfig config(String apiKey, String ruc, boolean modoSimulado) {
        FactuSmartConfig config = new FactuSmartConfig();
        config.setApiKey(apiKey);
        config.setRuc(ruc);
        config.setModoSimulado(modoSimulado);
        return config;
    }
}
