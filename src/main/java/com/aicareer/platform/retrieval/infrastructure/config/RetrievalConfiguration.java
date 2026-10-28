package com.aicareer.platform.retrieval.infrastructure.config;

import java.net.http.HttpClient;

import com.aicareer.platform.retrieval.application.EmbeddingClient;
import com.aicareer.platform.retrieval.infrastructure.embedding.GeminiEmbeddingClient;
import com.aicareer.platform.retrieval.infrastructure.embedding.OllamaEmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetrievalConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RetrievalConfiguration.class);

    @Bean
    public EmbeddingClient embeddingClient(
            HttpClient aiHttpClient,
            EmbeddingProviderProperties providerProperties,
            OllamaEmbeddingProperties properties,
            GeminiEmbeddingProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        EmbeddingClient client = switch (providerProperties.provider().trim().toLowerCase()) {
            case "ollama" -> new OllamaEmbeddingClient(aiHttpClient, properties, objectMapper);
            case "gemini" -> new GeminiEmbeddingClient(aiHttpClient, geminiProperties, objectMapper);
            default -> throw new IllegalStateException(
                    "Unsupported retrieval embedding provider: " + providerProperties.provider()
            );
        };
        log.info(
                "Retrieval embedding configured provider={} model={} dimensions={} endpoint={}",
                providerProperties.provider(),
                client.model(),
                client.dimensions(),
                providerProperties.provider().equalsIgnoreCase("gemini")
                        ? geminiProperties.endpoint()
                        : properties.endpoint()
        );
        return client;
    }
}
