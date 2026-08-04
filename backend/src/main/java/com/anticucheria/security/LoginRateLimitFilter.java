package com.anticucheria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evita fuerza bruta en el login. Un mapa en memoria basta para un solo nodo
 * (Cloud Run con 1 instancia o una VM). Si escalas a varias instancias, hay que
 * pasar esto a Redis; mientras tanto corta el ataque más común desde una IP.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_INTENTOS = 20;
    private static final long VENTANA_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Ventana> porIp = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().endsWith("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = ipDe(request);
        long ahora = Instant.now().toEpochMilli();
        limpiarViejas(ahora);

        Ventana ventana = porIp.compute(ip, (clave, actual) -> {
            if (actual == null || ahora - actual.inicioMs > VENTANA_MS) {
                return new Ventana(ahora, 1);
            }
            actual.intentos++;
            return actual;
        });

        if (ventana.intentos > MAX_INTENTOS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"mensaje\":\"Demasiados intentos de ingreso. Espera unos minutos.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String ipDe(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();
    }

    private void limpiarViejas(long ahora) {
        if (porIp.size() < 500) {
            return;
        }
        Iterator<Map.Entry<String, Ventana>> it = porIp.entrySet().iterator();
        while (it.hasNext()) {
            if (ahora - it.next().getValue().inicioMs > VENTANA_MS) {
                it.remove();
            }
        }
    }

    private static final class Ventana {
        private final long inicioMs;
        private int intentos;

        private Ventana(long inicioMs, int intentos) {
            this.inicioMs = inicioMs;
            this.intentos = intentos;
        }
    }
}
