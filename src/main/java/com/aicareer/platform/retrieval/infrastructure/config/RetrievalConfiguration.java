package com.aicareer.platform.retrieval.infrastructure.config;

import java.net.http.HttpClient;

import com.aicareer.platform.retrieval.application.EmbeddingClient;
import com.aicareer.platform.retrieval.infrastructure.embedding.GeminiEmbeddingClient;
import com.aicareer.platform.retrieval.infrastructure.embedding.OllamaEmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetrievalConfiguration {

    @Bean
    public EmbeddingClient embeddingClient(
            HttpClient aiHttpClient,
            EmbeddingProviderProperties providerProperties,
            OllamaEmbeddingProperties properties,
            GeminiEmbeddingProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        return switch (providerProperties.provider().trim().toLowerCase()) {
            case "ollama" -> new OllamaEmbeddingClient(aiHttpClient, properties, objectMapper);
            case "gemini" -> new GeminiEmbeddingClient(aiHttpClient, geminiProperties, objectMapper);
            default -> throw new IllegalStateException(
                    "Unsupported retrieval embedding provider: " + providerProperties.provider()
            );
        };
    }
}
