package com.aicareer.platform.ai.infrastructure.config;

import java.net.http.HttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.aicareer.platform.ai.application.LlmClient;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.FallbackLlmResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.GeminiResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.JobAnalysisPrompt;
import com.aicareer.platform.ai.infrastructure.llm.JobMatchPrompt;
import com.aicareer.platform.ai.infrastructure.llm.GeminiImageTextExtractor;
import com.aicareer.platform.ai.infrastructure.llm.OllamaChatClient;
import com.aicareer.platform.ai.infrastructure.llm.OpenAiResponsesClient;
import com.aicareer.platform.ai.infrastructure.llm.ResumeDraftPrompt;
import com.aicareer.platform.job.application.JobPostingImageTextExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    public HttpClient aiHttpClient(AiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
    }

    @Bean
    public JobPostingImageTextExtractor jobPostingImageTextExtractor(
            HttpClient aiHttpClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        return new GeminiImageTextExtractor(
                aiHttpClient,
                geminiProperties,
                objectMapper
        );
    }

    @Bean
    public LlmClient llmClient(
            HttpClient aiHttpClient,
            AiProperties aiProperties,
            OpenAiProperties properties,
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper,
            JobAnalysisPrompt prompt,
            JobMatchPrompt matchPrompt,
            ResumeDraftPrompt resumeDraftPrompt,
            ResumeDraftProperties resumeDraftProperties
    ) {
        return switch (aiProperties.provider().trim().toLowerCase()) {
            case "openai" -> new OpenAiResponsesClient(
                    aiHttpClient,
                    aiProperties,
                    properties,
                    objectMapper,
                    prompt,
                    matchPrompt,
                    resumeDraftPrompt,
                    resumeDraftProperties.maxOutputTokens(),
                    resumeDraftProperties.maxAttempts()
            );
            case "ollama" -> new OllamaChatClient(
                    aiHttpClient,
                    aiProperties,
                    ollamaProperties,
                    objectMapper,
                    prompt,
                    matchPrompt,
                    resumeDraftPrompt,
                    resumeDraftProperties.maxOutputTokens(),
                    resumeDraftProperties.maxAttempts()
            );
            default -> throw new IllegalStateException(
                    "Unsupported AI provider: " + aiProperties.provider()
            );
        };
    }

    @Bean
    @Primary
    public LlmResumeDraftClient resumeDraftLlmClient(
            LlmClient primaryClient,
            HttpClient aiHttpClient,
            GeminiProperties geminiProperties,
            ResumeDraftProperties resumeDraftProperties,
            ObjectMapper objectMapper,
            ResumeDraftPrompt resumeDraftPrompt
    ) {
        if (!(primaryClient instanceof LlmResumeDraftClient primaryResumeDraftClient)) {
            throw new IllegalStateException("Configured AI provider does not support resume drafts");
        }
        LlmResumeDraftClient geminiClient = new GeminiResumeDraftClient(
                aiHttpClient,
                geminiProperties,
                resumeDraftProperties,
                objectMapper,
                resumeDraftPrompt
        );
        String resumeDraftProvider = resumeDraftProperties.provider().trim().toLowerCase();
        return switch (resumeDraftProvider) {
            case "gemini" -> new FallbackLlmResumeDraftClient(
                    geminiClient,
                    primaryResumeDraftClient,
                    true
            );
            case "configured", "ollama", "openai" -> new FallbackLlmResumeDraftClient(
                    primaryResumeDraftClient,
                    geminiClient,
                    geminiConfigured(geminiProperties)
            );
            default -> throw new IllegalStateException(
                    "Unsupported resume draft provider: " + resumeDraftProperties.provider()
            );
        };
    }

    private boolean geminiConfigured(GeminiProperties properties) {
        return properties.apiKey() != null && !properties.apiKey().isBlank();
    }

    @Bean
    public JobAnalysisPrompt jobAnalysisPrompt(AiProperties properties) {
        ClassPathResource resource = new ClassPathResource(
                "ai/prompts/" + properties.promptVersion() + ".txt"
        );
        try {
            return new JobAnalysisPrompt(
                    properties.promptVersion(),
                    resource.getContentAsString(StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load AI prompt " + properties.promptVersion(), exception);
        }
    }

    @Bean
    public JobMatchPrompt jobMatchPrompt(RagProperties properties) {
        ClassPathResource resource = new ClassPathResource(
                "ai/prompts/" + properties.promptVersion() + ".txt"
        );
        try {
            return new JobMatchPrompt(
                    properties.promptVersion(),
                    resource.getContentAsString(StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load AI matching prompt " + properties.promptVersion(), exception);
        }
    }

    @Bean
    public ResumeDraftPrompt resumeDraftPrompt(ResumeDraftProperties properties) {
        ClassPathResource resource = new ClassPathResource(
                "ai/prompts/" + properties.promptVersion() + ".txt"
        );
        try {
            return new ResumeDraftPrompt(
                    properties.promptVersion(),
                    resource.getContentAsString(StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load resume draft prompt " + properties.promptVersion(),
                    exception
            );
        }
    }
}
