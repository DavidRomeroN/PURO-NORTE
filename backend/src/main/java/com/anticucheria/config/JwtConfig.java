package com.anticucheria.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Resuelve la clave con la que se firman las sesiones y se niega a arrancar si no hay una
 * de fiar.
 *
 * El secreto no puede vivir en el repositorio. Quien lo conozca puede firmarse un token
 * con rol de administrador y entrar sin contrasena, asi que un valor por defecto en
 * application.properties equivale a publicar la llave del negocio en internet.
 *
 * La comprobacion vive aca, junto a donde se construye la clave, y no en una clase aparte,
 * para que no exista un orden de arranque en el que la clave se use antes de validarla.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Slf4j
public class JwtConfig implements InitializingBean {

    /**
     * HS256 firma con 256 bits, o sea 32 bytes. Con menos, jjwt lanza WeakKeyException y
     * el arranque muere con un mensaje que no dice como arreglarlo.
     */
    public static final int LONGITUD_MINIMA = 32;

    private String secret;

    /** Ocho horas: un turno completo, para que a nadie se le cierre la sesion cobrando. */
    private long expirationMs = 28800000;

    /**
     * Solo dev. Sin secreto definido se genera uno al azar en cada arranque: cierra las
     * sesiones abiertas al reiniciar, pero permite trabajar en local sin que exista
     * ninguna clave de firma escrita en el proyecto.
     */
    private boolean secretoEfimero = false;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private SecretKey clave;

    @Override
    public void afterPropertiesSet() {
        clave = resolverClave();
    }

    public SecretKey clave() {
        return clave;
    }

    private SecretKey resolverClave() {
        if (secret != null && !secret.isBlank()) {
            String limpio = secret.trim();
            exigirLargoSuficiente(limpio);
            return Keys.hmacShaKeyFor(limpio.getBytes(StandardCharsets.UTF_8));
        }
        if (!secretoEfimero) {
            throw new ConfiguracionInseguraException(
                    "No hay con que firmar las sesiones: jwt.secret esta vacia.",
                    """
                    Define la variable de entorno JWT_SECRET con una cadena aleatoria de al \
                    menos %d caracteres y vuelve a arrancar. Para generarla:

                        PowerShell: [Convert]::ToBase64String([byte[]](1..48 | ForEach-Object { Get-Random -Maximum 256 }))
                        Linux:      openssl rand -base64 48

                    Guardala fuera del repositorio y no la compartas: quien conozca ese valor \
                    puede firmarse un token de administrador y entrar al sistema sin contrasena.

                    Para trabajar en local sin definirla: SPRING_PROFILES_ACTIVE=dev, que genera \
                    un secreto al azar en cada arranque."""
                            .formatted(LONGITUD_MINIMA));
        }

        log.warn("Secreto de sesiones efimero: se genero uno al azar y las sesiones abiertas "
                + "se cierran en cada reinicio. Solo vale para desarrollo.");
        return Jwts.SIG.HS256.key().build();
    }

    private void exigirLargoSuficiente(String secreto) {
        if (secreto.length() >= LONGITUD_MINIMA) {
            return;
        }
        throw new ConfiguracionInseguraException(
                "El secreto de las sesiones es demasiado corto: JWT_SECRET tiene %d caracteres y "
                        .formatted(secreto.length())
                        + "hacen falta al menos %d.".formatted(LONGITUD_MINIMA),
                """
                Los tokens se firman con HS256, que necesita una clave de 256 bits. Una clave \
                corta se puede adivinar por fuerza bruta y con ella cualquiera se hace \
                administrador.

                Alarga JWT_SECRET a %d caracteres o mas, preferiblemente aleatorios."""
                        .formatted(LONGITUD_MINIMA));
    }
}
