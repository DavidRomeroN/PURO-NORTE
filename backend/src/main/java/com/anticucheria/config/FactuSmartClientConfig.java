package com.anticucheria.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
@RequiredArgsConstructor
public class FactuSmartClientConfig {

    /**
     * El local esta en Puno y el servidor puede estar en UTC. Sin fijar la zona, una
     * boleta emitida a las 8pm se registraria con la fecha del dia siguiente.
     */
    public static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private final FactuSmartConfig config;

    @Bean
    public Clock relojPeru() {
        return Clock.system(ZONA_PERU);
    }

    @Bean
    public RestClient factuSmartRestClient() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(config.getTimeoutMs()));
        fabrica.setReadTimeout(Duration.ofMillis(config.getTimeoutMs()));

        return RestClient.builder()
                .baseUrl(config.getUrlBase() == null ? "" : config.getUrlBase())
                .requestFactory(fabrica)
                .defaultHeader("X-API-Key", config.getApiKey() == null ? "" : config.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                // El cliente nunca lanza por status: cada codigo se interpreta en el servicio.
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();
    }
}
