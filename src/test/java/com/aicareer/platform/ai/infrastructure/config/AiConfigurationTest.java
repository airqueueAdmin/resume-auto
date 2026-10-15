package com.aicareer.platform.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.aicareer.platform.ai.application.LlmClient;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.ResumeDraftPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiConfigurationTest {

    @Test
    void selectsGeminiAsTheDefaultResumeDraftProvider() {
        LlmClient configuredClient = configuredResumeDraftClient("ollama");

        LlmResumeDraftClient selected = new AiConfiguration().resumeDraftLlmClient(
                configuredClient,
                HttpClient.newHttpClient(),
                geminiProperties(""),
                resumeDraftProperties("gemini"),
                new ObjectMapper(),
                new ResumeDraftPrompt("resume-draft-v4", "system")
        );

        assertThat(selected.provider()).isEqualTo("gemini");
    }

    @Test
    void canKeepTheConfiguredGlobalProviderAsTheResumeDraftPrimary() {
        LlmClient configuredClient = configuredResumeDraftClient("ollama");

        LlmResumeDraftClient selected = new AiConfiguration().resumeDraftLlmClient(
                configuredClient,
                HttpClient.newHttpClient(),
                geminiProperties(""),
                resumeDraftProperties("configured"),
                new ObjectMapper(),
                new ResumeDraftPrompt("resume-draft-v4", "system")
        );

        assertThat(selected.provider()).isEqualTo("ollama");
    }

    private LlmClient configuredResumeDraftClient(String provider) {
        LlmClient client = mock(
                LlmClient.class,
                Mockito.withSettings().extraInterfaces(LlmResumeDraftClient.class)
        );
        when(client.provider()).thenReturn(provider);
        return client;
    }

    private ResumeDraftProperties resumeDraftProperties(String provider) {
        return new ResumeDraftProperties(provider, "resume-draft-v4", 8, 12000, 450, 512, 1);
    }

    private GeminiProperties geminiProperties(String apiKey) {
        return new GeminiProperties(
                URI.create("http://localhost:9999/v1beta"),
                apiKey,
                "gemini-test",
                Duration.ofSeconds(2),
                1,
                Duration.ZERO,
                4096
        );
    }
}
