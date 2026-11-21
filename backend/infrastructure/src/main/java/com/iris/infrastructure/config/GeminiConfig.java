package com.iris.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crea DOS modelos de embeddings de Gemini (construcción manual, según la documentación de Spring AI):
 * mismo modelo y mismas dimensiones, distinto taskType.
 */
@Configuration

@EnableConfigurationProperties(EmbeddingsPropiedades.class)
public class GeminiConfig {

    @Bean
    GoogleGenAiEmbeddingConnectionDetails conexionGemini(EmbeddingsPropiedades p) {
        // Con api-key, Spring AI usa la Gemini Developer API (no Vertex AI)
        return GoogleGenAiEmbeddingConnectionDetails.builder().apiKey(p.apiKey()).build();
    }

    /** Para fragmentos de documentos: "esto es contenido que alguien va a buscar". */
    @Bean
    EmbeddingModel embeddingsDocumentos(GoogleGenAiEmbeddingConnectionDetails conexion, EmbeddingsPropiedades p) {
        return new GoogleGenAiTextEmbeddingModel(conexion, opciones(p, TaskType.RETRIEVAL_DOCUMENT));
    }

    /** Para preguntas del usuario: "esto es una búsqueda". */
    @Bean
    EmbeddingModel embeddingsConsultas(GoogleGenAiEmbeddingConnectionDetails conexion, EmbeddingsPropiedades p) {
        return new GoogleGenAiTextEmbeddingModel(conexion, opciones(p, TaskType.RETRIEVAL_QUERY));
    }

    private static GoogleGenAiTextEmbeddingOptions opciones(EmbeddingsPropiedades p, TaskType tipo) {
        return GoogleGenAiTextEmbeddingOptions.builder()
                .model(p.modelo())
                .taskType(tipo)
                .dimensions(p.dimensiones()) // MRL: recorta de 3072 a 1536
                .build();
    }
}
