package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloque "iris.correo": el interruptor del correo y quién aparece como remitente. */
@ConfigurationProperties(prefix = "iris.correo")
public record CorreoPropiedades(boolean habilitado, String remitente) {
}
