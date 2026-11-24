package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloque "iris.consulta": ajustes del chat con Iris. */
@ConfigurationProperties(prefix = "iris.consulta")
public record ConsultaPropiedades(double umbralSimilitud) {

    public ConsultaPropiedades {
        if (umbralSimilitud <= 0 || umbralSimilitud >= 1) {
            throw new IllegalStateException("iris.consulta.umbral-similitud debe estar entre 0 y 1");
        }
    }
}
