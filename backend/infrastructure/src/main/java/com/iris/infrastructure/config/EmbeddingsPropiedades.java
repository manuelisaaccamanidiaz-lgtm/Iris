package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloque "iris.ia.embeddings" del application.yml. */
@ConfigurationProperties(prefix = "iris.ia.embeddings")
public record EmbeddingsPropiedades(String apiKey, String modelo, int dimensiones) {

    public EmbeddingsPropiedades {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("pon_tu_clave")) {
            throw new IllegalStateException("Falta GEMINI_API_KEY en el .env");
        }
        if (dimensiones <= 0 || dimensiones > 2000) {
            // 2000 = límite del índice HNSW de pgvector para el tipo vector
            throw new IllegalStateException("Las dimensiones deben estar entre 1 y 2000");
        }
    }
}
