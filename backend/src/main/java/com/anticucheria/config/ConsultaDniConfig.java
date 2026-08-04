package com.anticucheria.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Servicio externo que resuelve el nombre de un DNI.
 *
 * FactuSmart no expone consulta de documentos: su API es solo de comprobantes. Por eso la
 * verificacion va contra un proveedor aparte, con su propio token y su propio saldo.
 *
 * Sin token configurado no se consulta nada y la caja lo dice claramente. Es una ayuda
 * para que el cajero confirme el numero en voz alta, nunca un requisito para cobrar: la
 * boleta se emite mandando solo el DNI, que es como la espera FactuSmart.
 */
@Configuration
@ConfigurationProperties(prefix = "consulta-dni")
@Getter
@Setter
public class ConsultaDniConfig {

    private String url;

    private String token;

    /** Corto a proposito: hay un cliente esperando en la caja. */
    private int timeoutMs = 5000;

    public boolean estaConfigurado() {
        return token != null && !token.isBlank();
    }
}
