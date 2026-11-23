package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bloque "iris.correo": el interruptor del correo, quién aparece como remitente y la dirección
 * de la app web (React) a la que apuntan los botones de los correos.
 */
@ConfigurationProperties(prefix = "iris.correo")
public record CorreoPropiedades(boolean habilitado, String remitente, String urlApp) {
}
