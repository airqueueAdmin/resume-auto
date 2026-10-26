package com.aicareer.platform.ai.infrastructure.config;

import java.net.http.HttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.aicareer.platform.ai.application.LlmClient;
import com.aicareer.platform.ai.application.LlmSelfIntroductionAnswerClient;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.FallbackLlmResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.FallbackLlmSelfIntroductionAnswerClient;
import com.aicareer.platform.ai.infrastructure.llm.GeminiResumeDraftClient;
import com.aicareer.platform.ai.infrastructure.llm.GeminiStructuredClient;
import com.aicareer.platform.ai.infrastructure.llm.JobAnalysisPrompt;
import com.aicareer.platform.ai.infrastructure.llm.JobMatchPrompt;
import com.aicareer.platform.ai.infrastructure.llm.GeminiImageTextExtractor;
import com.aicareer.platform.ai.infrastructure.llm.OllamaChatClient;
import com.aicareer.platform.ai.infrastructure.llm.OpenAiResponsesClient;
import com.aicareer.platform.ai.infrastructure.llm.ResumeDraftPrompt;
import com.aicareer.platform.ai.infrastructure.llm.SelfIntroductionPrompt;
import com.aicareer.platform.job.application.JobPostingImageTextExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiConfiguration.class);

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
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper,
            JobAnalysisPrompt prompt,
            JobMatchPrompt matchPrompt,
            ResumeDraftPrompt resumeDraftPrompt,
            ResumeDraftProperties resumeDraftProperties,
            SelfIntroductionPrompt selfIntroductionPrompt,
            SelfIntroductionProperties selfIntroductionProperties
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
                    resumeDraftProperties.maxAttempts(),
                    selfIntroductionPrompt,
                    selfIntroductionProperties.maxOutputTokens(),
                    selfIntroductionProperties.maxAttempts()
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
                    resumeDraftProperties.maxAttempts(),
                    selfIntroductionPrompt,
                    selfIntroductionProperties.maxOutputTokens(),
                    selfIntroductionProperties.maxAttempts()
            );
            case "gemini" -> new GeminiStructuredClient(
                    aiHttpClient,
                    aiProperties,
                    geminiProperties,
                    objectMapper,
                    prompt,
                    matchPrompt,
                    resumeDraftProperties,
                    resumeDraftPrompt,
                    selfIntroductionPrompt,
                    selfIntroductionProperties
            );
            default -> throw new IllegalStateException(
                    "Unsupported AI provider: " + aiProperties.provider()
            );
        };
    }

    @Bean
    public LlmSelfIntroductionAnswerClient selfIntroductionAnswerLlmClient(
            LlmClient client,
            HttpClient aiHttpClient,
            AiProperties aiProperties,
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper,
            JobAnalysisPrompt prompt,
            JobMatchPrompt matchPrompt,
            ResumeDraftProperties resumeDraftProperties,
            ResumeDraftPrompt resumeDraftPrompt,
            SelfIntroductionPrompt selfIntroductionPrompt,
            SelfIntroductionProperties selfIntroductionProperties
    ) {
        if (!(client instanceof LlmSelfIntroductionAnswerClient selfIntroductionClient)) {
            throw new IllegalStateException("Configured AI provider does not support self-introduction answers");
        }
        AiProperties ollamaRequestProperties = new AiProperties(
                "ollama", aiProperties.promptVersion(), selfIntroductionProperties.ollamaTimeout(),
                1, aiProperties.retryDelay(), aiProperties.maxOutputTokens(), aiProperties.temperature()
        );
        LlmSelfIntroductionAnswerClient ollamaFallback = new OllamaChatClient(
                aiHttpClient,
                ollamaRequestProperties,
                ollamaProperties,
                objectMapper,
                prompt,
                matchPrompt,
                resumeDraftPrompt,
                resumeDraftProperties.maxOutputTokens(),
                resumeDraftProperties.maxAttempts(),
                selfIntroductionPrompt,
                selfIntroductionProperties.ollamaMaxOutputTokens(),
                1
        );
        boolean fallbackEnabled = selfIntroductionProperties.ollamaFallbackEnabled()
                && "gemini".equalsIgnoreCase(selfIntroductionClient.provider());
        log.info("Self-introduction AI configured primaryProvider={} fallbackProvider={} fallbackEnabled={} promptVersion={} minimumSimilarity={} ollamaTimeout={} ollamaMaxOutputTokens={}",
                selfIntroductionClient.provider(),
                ollamaFallback.provider(),
                fallbackEnabled,
                selfIntroductionPrompt.version(),
                selfIntroductionProperties.minimumSimilarity(),
                selfIntroductionProperties.ollamaTimeout(),
                selfIntroductionProperties.ollamaMaxOutputTokens());
        return new FallbackLlmSelfIntroductionAnswerClient(
                selfIntroductionClient,
                ollamaFallback,
                fallbackEnabled
        );
    }

    @Bean
    @Primary
    public LlmResumeDraftClient resumeDraftLlmClient(
            LlmClient primaryClient,
            HttpClient aiHttpClient,
            AiProperties aiProperties,
            OllamaProperties ollamaProperties,
            GeminiProperties geminiProperties,
            ResumeDraftProperties resumeDraftProperties,
            ObjectMapper objectMapper,
            JobAnalysisPrompt jobAnalysisPrompt,
            JobMatchPrompt jobMatchPrompt,
            ResumeDraftPrompt resumeDraftPrompt
    ) {
        LlmResumeDraftClient primaryResumeDraftClient = primaryClient instanceof LlmResumeDraftClient client
                ? client
                : null;
        LlmResumeDraftClient geminiClient = new GeminiResumeDraftClient(
                aiHttpClient,
                geminiProperties,
                resumeDraftProperties,
                objectMapper,
                resumeDraftPrompt
        );
        AiProperties ollamaRequestProperties = new AiProperties(
                "ollama",
                aiProperties.promptVersion(),
                resumeDraftProperties.ollamaTimeout(),
                1,
                aiProperties.retryDelay(),
                resumeDraftProperties.maxOutputTokens(),
                aiProperties.temperature()
        );
        LlmResumeDraftClient ollamaClient = new OllamaChatClient(
                aiHttpClient,
                ollamaRequestProperties,
                ollamaProperties,
                objectMapper,
                jobAnalysisPrompt,
                jobMatchPrompt,
                resumeDraftPrompt,
                resumeDraftProperties.maxOutputTokens(),
                1
        );
        String resumeDraftProvider = resumeDraftProperties.provider().trim().toLowerCase();
        LlmResumeDraftClient selected = switch (resumeDraftProvider) {
            case "gemini" -> {
                boolean configuredProviderIsDifferent = primaryResumeDraftClient != null
                        && !"gemini".equals(primaryResumeDraftClient.provider());
                LlmResumeDraftClient fallback = configuredProviderIsDifferent
                        ? primaryResumeDraftClient
                        : ollamaClient;
                yield new FallbackLlmResumeDraftClient(
                        geminiClient,
                        fallback,
                        configuredProviderIsDifferent || resumeDraftProperties.ollamaFallbackEnabled()
                );
            }
            case "configured", "ollama", "openai" -> {
                if (primaryResumeDraftClient == null) {
                    throw new IllegalStateException("Configured AI provider does not support resume drafts");
                }
                yield new FallbackLlmResumeDraftClient(
                        primaryResumeDraftClient,
                        geminiClient,
                        geminiConfigured(geminiProperties)
                                && !"gemini".equals(primaryResumeDraftClient.provider())
                );
            }
            default -> throw new IllegalStateException(
                    "Unsupported resume draft provider: " + resumeDraftProperties.provider()
            );
        };
        log.info("Resume draft AI configured primaryProvider={} fallbackEnabled={} promptVersion={} maxOutputTokens={} ollamaTimeout={}",
                selected.provider(),
                resumeDraftProperties.ollamaFallbackEnabled(),
                resumeDraftPrompt.version(),
                resumeDraftProperties.maxOutputTokens(),
                resumeDraftProperties.ollamaTimeout());
        return selected;
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

    @Bean
    public SelfIntroductionPrompt selfIntroductionPrompt(SelfIntroductionProperties properties) {
        ClassPathResource resource = new ClassPathResource(
                "ai/prompts/" + properties.promptVersion() + ".txt"
        );
        try {
            return new SelfIntroductionPrompt(
                    properties.promptVersion(),
                    resource.getContentAsString(StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load self-introduction prompt " + properties.promptVersion(),
                    exception
            );
        }
    }
}
