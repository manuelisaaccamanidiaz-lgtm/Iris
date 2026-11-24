package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloque "iris.ia.chat" del application.yml (el modelo que redacta las respuestas). */
@ConfigurationProperties(prefix = "iris.ia.chat")
public record ChatPropiedades(String apiKey, String baseUrl, String modelo, double temperatura, int maxTokens) {

    public ChatPropiedades {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("pon_tu_clave")) {
            throw new IllegalStateException("Falta GROQ_API_KEY en el .env");
        }
    }
}
