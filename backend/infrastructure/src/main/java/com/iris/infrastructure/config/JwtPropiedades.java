package com.iris.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee el bloque "iris.jwt" del application.yml y lo convierte en un objeto tipado.
 * Si la clave es muy corta, la app NO arranca: mejor fallar al inicio que firmar tokens débiles.
 */
@ConfigurationProperties(prefix = "iris.jwt")
public record JwtPropiedades(String secreto, Duration duracion) {

    public JwtPropiedades {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 caracteres (256 bits) para HS256. Revisa tu .env");
        }
        if (duracion == null) {
            duracion = Duration.ofHours(8);
        }
    }
}
