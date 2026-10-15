package com.aicareer.platform.retrieval.infrastructure.config;

import java.net.http.HttpClient;

import com.aicareer.platform.retrieval.application.EmbeddingClient;
import com.aicareer.platform.retrieval.infrastructure.embedding.OllamaEmbeddingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetrievalConfiguration {

    @Bean
    public EmbeddingClient embeddingClient(
            HttpClient aiHttpClient,
            OllamaEmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        return new OllamaEmbeddingClient(aiHttpClient, properties, objectMapper);
    }
}
