package com.aicareer.platform.ai.infrastructure.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.LlmResumeDraftClient;
import com.aicareer.platform.ai.application.LlmResumeDraftCompletion;
import com.aicareer.platform.ai.application.LlmUsage;
import com.aicareer.platform.ai.application.RetrievedContext;
import com.aicareer.platform.ai.application.ResumeDraftDocument;
import com.aicareer.platform.ai.infrastructure.config.GeminiProperties;
import com.aicareer.platform.ai.infrastructure.config.ResumeDraftProperties;
import com.aicareer.platform.job.domain.JobPosting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Uses the same Gemini generateContent API as OCR, but for text-only grounded resume drafts.
 */
public class GeminiResumeDraftClient implements LlmResumeDraftClient {

    private static final String PROVIDER = "gemini";

    private final HttpClient httpClient;
    private final GeminiProperties properties;
    private final ResumeDraftProperties resumeDraftProperties;
    private final ObjectMapper objectMapper;
    private final ResumeDraftPrompt prompt;

    public GeminiResumeDraftClient(
            HttpClient httpClient,
            GeminiProperties properties,
            ResumeDraftProperties resumeDraftProperties,
            ObjectMapper objectMapper,
            ResumeDraftPrompt prompt
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.resumeDraftProperties = resumeDraftProperties;
        this.objectMapper = objectMapper;
        this.prompt = prompt;
    }

    @Override
    public LlmResumeDraftCompletion generateResumeDraft(JobPosting jobPosting, List<RetrievedContext> contexts) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw AiIntegrationException.unavailable("Gemini API key is not configured");
        }

        String requestId = UUID.randomUUID().toString();
        String requestBody = createRequestBody(jobPosting, contexts);
        int maxAttempts = Math.max(1, resumeDraftProperties.maxAttempts());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = send(requestBody, requestId);
                if (response.statusCode() == 200) {
                    return parseResponse(response.body(), requestId);
                }
                if (isRetryableStatus(response.statusCode()) && attempt < maxAttempts) {
                    waitBeforeRetry();
                    continue;
                }
                if (isRetryableStatus(response.statusCode())) {
                    throw AiIntegrationException.unavailable("Gemini resume draft request failed after retries");
                }
                throw AiIntegrationException.requestFailed("Gemini rejected the resume draft request");
            } catch (HttpTimeoutException exception) {
                if (attempt < maxAttempts) {
                    waitBeforeRetry();
                    continue;
                }
                throw AiIntegrationException.unavailable("Gemini resume draft request timed out", exception);
            } catch (IOException exception) {
                if (attempt < maxAttempts) {
                    waitBeforeRetry();
                    continue;
                }
                throw AiIntegrationException.unavailable("Gemini resume draft request could not be completed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw AiIntegrationException.unavailable("Gemini resume draft request was interrupted", exception);
            } catch (AiIntegrationException exception) {
                if (exception.kind() == AiIntegrationException.Kind.INVALID_RESPONSE
                        && attempt < maxAttempts) {
                    waitBeforeRetry();
                    continue;
                }
                throw exception;
            }
        }

        throw AiIntegrationException.unavailable("Gemini resume draft request could not be completed");
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public String resumeDraftPromptVersion() {
        return prompt.version();
    }

    private HttpResponse<String> send(String requestBody, String requestId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(properties.timeout())
                .header("x-goog-api-key", properties.apiKey())
                .header("Content-Type", "application/json")
                .header("X-Client-Request-Id", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI endpoint() {
        String base = properties.endpoint().toString().replaceAll("/+$", "");
        return URI.create(base + "/models/" + properties.model() + ":generateContent");
    }

    private String createRequestBody(JobPosting jobPosting, List<RetrievedContext> contexts) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", prompt.systemMessage()))
        ));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userInput(jobPosting, contexts)))
        )));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", resumeDraftProperties.maxOutputTokens());
        generationConfig.put("thinkingConfig", Map.of(
                "thinkingBudget", resumeDraftProperties.geminiThinkingBudget()
        ));
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", ResumeDraftSchema.definition());
        body.put("generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw AiIntegrationException.requestFailed("Could not create Gemini resume draft request", exception);
        }
    }

    private String userInput(JobPosting jobPosting, List<RetrievedContext> contexts) {
        String evidence = contexts.stream()
                .map(context -> "[" + context.citationId() + "] source_type=" + context.sourceType()
                        + " similarity=" + context.similarity() + "\n"
                        + "<career_evidence>\n"
                        + context.promptContent()
                        + "\n</career_evidence>")
                .collect(java.util.stream.Collectors.joining("\n"));
        return "Create a detailed resume draft tailored to this job posting using only the retrieved career evidence.\n"
                + "The selfIntroductionSections must contain exactly 3 distinct Korean appeal topics. Each title must express a role-relevant candidate strength within 50 characters, and each content must have at least 3 complete, evidence-grounded sentences.\n"
                + "Job posting content begins below.\n"
                + "<job_posting>\n"
                + "Title: " + jobPosting.getTitle() + "\n"
                + "Company: " + jobPosting.getCompanyName() + "\n"
                + jobPosting.getDescription() + "\n"
                + "</job_posting>\n"
                + "Job posting content ends above.\n"
                + "Retrieved career evidence begins below.\n"
                + evidence + "\n"
                + "Retrieved career evidence ends above. Use only exact citation IDs from this evidence.";
    }

    private LlmResumeDraftCompletion parseResponse(String body, String requestId) {
        String finishReason = "";
        try {
            JsonNode root = objectMapper.readTree(body);
            finishReason = root.path("candidates").path(0).path("finishReason").asText("");
            String outputText = responseText(root);
            if (outputText.isBlank()) {
                throw new IllegalArgumentException("Gemini response text is missing");
            }
            ResumeDraftDocument draft = objectMapper.readValue(normalizeJson(outputText), ResumeDraftDocument.class);
            JsonNode usage = root.path("usageMetadata");
            long inputTokens = nonNegativeLongOrZero(usage, "promptTokenCount");
            long outputTokens = nonNegativeLongOrZero(usage, "candidatesTokenCount");
            long totalTokens = nonNegativeLongOrZero(usage, "totalTokenCount");
            totalTokens = totalTokens > 0
                    ? totalTokens
                    : inputTokens + outputTokens;
            return new LlmResumeDraftCompletion(
                    draft,
                    new LlmUsage(inputTokens, outputTokens, totalTokens),
                    "gemini-" + requestId
            );
        } catch (Exception exception) {
            throw AiIntegrationException.invalidResponse(
                    "Gemini returned an invalid resume draft response"
                            + (finishReason.isBlank() ? "" : " (finishReason=" + finishReason + ")"),
                    exception
            );
        }
    }

    private String responseText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode candidate : root.path("candidates")) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                String value = part.path("text").asText("");
                if (!value.isBlank()) {
                    text.append(value);
                }
            }
        }
        return text.toString().trim();
    }

    private String normalizeJson(String outputText) {
        String normalized = outputText.trim();
        if (normalized.startsWith("```")) {
            int firstLineEnd = normalized.indexOf('\n');
            int closingFence = normalized.lastIndexOf("```");
            if (firstLineEnd >= 0 && closingFence > firstLineEnd) {
                normalized = normalized.substring(firstLineEnd + 1, closingFence).trim();
            }
        }

        if (normalized.startsWith("{") && normalized.endsWith("}")) {
            return normalized;
        }

        int objectStart = normalized.indexOf('{');
        int objectEnd = normalized.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return normalized.substring(objectStart, objectEnd + 1).trim();
        }
        return normalized;
    }

    private long nonNegativeLongOrZero(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isIntegralNumber() || value.longValue() < 0) {
            return 0;
        }
        return value.longValue();
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode >= 500;
    }

    private void waitBeforeRetry() {
        if (!properties.retryDelay().isZero() && !properties.retryDelay().isNegative()) {
            LockSupport.parkNanos(properties.retryDelay().toNanos());
        }
    }
}
