package com.iris.infrastructure.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crea el modelo de chat de Groq con construcción manual (según la documentación de Spring AI):
 * el cliente de OpenAI, pero apuntando a la URL de Groq.
 */
@Configuration
@EnableConfigurationProperties(ChatPropiedades.class)
public class GroqConfig {

    @Bean
    OpenAiChatModel chatGroq(ChatPropiedades p) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(p.baseUrl())
                        .apiKey(p.apiKey())
                        .model(p.modelo())
                        .temperature(p.temperatura())
                        .maxTokens(p.maxTokens())
                        .build())
                .build();
    }
}
