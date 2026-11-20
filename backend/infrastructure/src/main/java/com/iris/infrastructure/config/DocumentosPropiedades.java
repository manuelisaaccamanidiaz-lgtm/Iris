package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Bloque "iris.documentos" del application.yml.
 * DataSize entiende textos como "20MB" y los convierte a bytes.
 */
@ConfigurationProperties(prefix = "iris.documentos")
public record DocumentosPropiedades(String directorioTemporal, DataSize tamanoMaximo) {

    public DocumentosPropiedades {
        if (tamanoMaximo == null) {
            tamanoMaximo = DataSize.ofMegabytes(20);
        }
    }
}
