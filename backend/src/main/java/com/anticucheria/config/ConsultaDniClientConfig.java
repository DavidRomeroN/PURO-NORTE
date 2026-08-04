package com.anticucheria.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ConsultaDniClientConfig {

    private final ConsultaDniConfig config;

    @Bean
    public RestClient apiPeruRestClient() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(config.getTimeoutMs()));
        fabrica.setReadTimeout(Duration.ofMillis(config.getTimeoutMs()));

        return RestClient.builder()
                .requestFactory(fabrica)
                .defaultHeader("Authorization", "Bearer " + (config.getToken() == null ? "" : config.getToken()))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                // Un 404 del proveedor significa "no figura ese DNI", que es un resultado
                // normal y no una excepcion. Cada codigo se interpreta en el servicio.
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();
    }
}
