package com.iris.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Orígenes (protocolo + dominio + puerto) a los que el navegador les permite llamar la API. */
@ConfigurationProperties(prefix = "iris.cors")
public record CorsPropiedades(List<String> origenes) {
}
