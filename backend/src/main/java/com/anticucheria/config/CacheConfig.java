package com.anticucheria.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Nombres ya resueltos de un DNI. Cada consulta cuesta un credito del proveedor y el
     * nombre de una persona no cambia, asi que repetir la busqueda del mismo numero seria
     * tirar dinero: en una anticucheria el mismo cliente vuelve.
     *
     * Un mapa en memoria basta. Los DNI distintos que ve el local en un ano caben de
     * sobra, y perder la cache al reiniciar no tiene ningun costo.
     */
    public static final String CACHE_DNI = "consultaDni";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(CACHE_DNI);
    }
}
