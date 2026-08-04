package com.anticucheria.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lo que se protege aca es que nunca exista una clave de firma conocida. Con la clave, un
 * atacante se emite un token con rol ADMIN y entra al sistema sin necesitar contrasena:
 * puede ver la caja, borrar pedidos y cambiar precios.
 */
class JwtConfigTest {

    private static final String SECRETO_VALIDO = "d7Xk2pQrs9LmZ4tVbN8cWyE3hJ6uA1oF";

    @Test
    @DisplayName("sin secreto y sin modo efimero, la aplicacion no arranca")
    void sinSecretoFalla() {
        assertThatThrownBy(() -> resolver(null, false))
                .isInstanceOf(ConfiguracionInseguraException.class)
                .hasMessageContaining("jwt.secret");

        // El caso real es la variable de entorno definida pero vacia.
        assertThatThrownBy(() -> resolver("   ", false))
                .isInstanceOf(ConfiguracionInseguraException.class);
    }

    @Test
    @DisplayName("un secreto corto se rechaza, aunque este definido")
    void secretoCortoFalla() {
        assertThat(SECRETO_VALIDO).hasSize(JwtConfig.LONGITUD_MINIMA);

        assertThatThrownBy(() -> resolver(SECRETO_VALIDO.substring(1), false))
                .isInstanceOf(ConfiguracionInseguraException.class)
                .hasMessageContaining("demasiado corto");

        // Y tampoco pasa en dev: si defines un secreto, tiene que servir.
        assertThatThrownBy(() -> resolver("corto", true))
                .isInstanceOf(ConfiguracionInseguraException.class);
    }

    @Test
    @DisplayName("con un secreto de 32 caracteres se firma con esa clave")
    void secretoValido() {
        assertThat(resolver(SECRETO_VALIDO, false)).isNotNull();

        // La misma cadena da siempre la misma clave: los tokens sobreviven a un reinicio.
        assertThat(resolver(SECRETO_VALIDO, false).getEncoded())
                .isEqualTo(resolver(SECRETO_VALIDO, false).getEncoded());
    }

    @Test
    @DisplayName("el espacio de sobra al pegar la variable no cambia la clave")
    void secretoConEspacios() {
        assertThat(resolver("  " + SECRETO_VALIDO + "  ", false).getEncoded())
                .isEqualTo(resolver(SECRETO_VALIDO, false).getEncoded());
    }

    @Test
    @DisplayName("en modo efimero se genera una clave distinta en cada arranque")
    void modoEfimero() {
        assertThat(resolver(null, true).getEncoded())
                .isNotEqualTo(resolver(null, true).getEncoded());
    }

    @Test
    @DisplayName("un secreto definido gana al modo efimero")
    void elSecretoGanaAlModoEfimero() {
        assertThat(resolver(SECRETO_VALIDO, true).getEncoded())
                .isEqualTo(resolver(SECRETO_VALIDO, false).getEncoded());
    }

    private SecretKey resolver(String secret, boolean secretoEfimero) {
        JwtConfig config = new JwtConfig();
        config.setSecret(secret);
        config.setSecretoEfimero(secretoEfimero);
        config.afterPropertiesSet();
        return config.clave();
    }
}
