package com.aicareer.platform.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.aicareer.platform.ai.application.LlmResumeDraftCompletion;
import com.aicareer.platform.ai.application.RetrievedContext;
import com.aicareer.platform.ai.infrastructure.config.GeminiProperties;
import com.aicareer.platform.ai.infrastructure.config.ResumeDraftProperties;
import com.aicareer.platform.identity.domain.User;
import com.aicareer.platform.job.domain.JobPosting;
import com.aicareer.platform.retrieval.domain.SourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeminiResumeDraftClientTest {

    private HttpServer server;
    private String requestBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsTextGenerationRequestAndParsesGroundedDraft() throws Exception {
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 200, responseBody());
        });

        GeminiResumeDraftClient client = new GeminiResumeDraftClient(
                HttpClient.newHttpClient(),
                new GeminiProperties(
                        URI.create("http://localhost:" + server.getAddress().getPort()),
                        "test-api-key",
                        "gemini-test",
                        Duration.ofSeconds(2),
                        1,
                        Duration.ZERO,
                        4096
                ),
                new ResumeDraftProperties("gemini", "resume-draft-v4", 8, 12000, 450, 512, 1),
                new ObjectMapper(),
                new ResumeDraftPrompt("resume-draft-v4", "Use only cited evidence.")
        );

        RetrievedContext context = new RetrievedContext(
                "C1",
                UUID.randomUUID(),
                SourceType.EXPERIENCE,
                UUID.randomUUID(),
                0,
                "Built APIs",
                "Built APIs",
                0.91,
                "nomic-embed-text"
        );
        LlmResumeDraftCompletion completion = client.generateResumeDraft(
                jobPosting(),
                List.of(context)
        );

        assertThat(completion.draft().headline().text()).isEqualTo("Backend Developer");
        assertThat(completion.usage().totalTokens()).isEqualTo(30);
        assertThat(requestBody).contains("systemInstruction", "responseMimeType", "Built APIs");
        JsonNode request = new ObjectMapper().readTree(requestBody);
        assertThat(request.path("generationConfig").path("responseSchema").path("properties")
                .path("experienceBullets").path("maxItems").asInt()).isEqualTo(2);
        assertThat(request.path("generationConfig").path("responseSchema").path("properties")
                .path("selfIntroductionSections").path("minItems").asInt()).isEqualTo(3);
        assertThat(request.path("generationConfig").path("responseSchema").path("properties")
                .path("selfIntroductionSections").path("maxItems").asInt()).isEqualTo(3);
        assertThat(request.path("generationConfig").path("thinkingConfig")
                .path("thinkingBudget").asInt()).isEqualTo(512);
    }

    @Test
    void parsesJsonCodeFenceWhenUsageMetadataIsMissing() throws Exception {
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String draft = "{\"headline\":{\"text\":\"Backend Developer\",\"citationIds\":[\"C1\"]},"
                    + "\"summary\":{\"text\":\"Builds APIs\",\"citationIds\":[\"C1\"]},"
                    + "\"experienceBullets\":[],\"projectBullets\":[],\"skills\":[]}";
            respond(exchange, 200, new ObjectMapper().writeValueAsString(Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of("parts", List.of(Map.of(
                                    "text", "```json\n" + draft + "\n```"
                            )))
                    ))
            )));
        });

        GeminiResumeDraftClient client = new GeminiResumeDraftClient(
                HttpClient.newHttpClient(),
                new GeminiProperties(
                        URI.create("http://localhost:" + server.getAddress().getPort()),
                        "test-api-key",
                        "gemini-test",
                        Duration.ofSeconds(2),
                        1,
                        Duration.ZERO,
                        4096
                ),
                new ResumeDraftProperties("gemini", "resume-draft-v4", 8, 12000, 800, 512, 1),
                new ObjectMapper(),
                new ResumeDraftPrompt("resume-draft-v4", "Use only cited evidence.")
        );

        LlmResumeDraftCompletion completion = client.generateResumeDraft(
                jobPosting(),
                List.of(new RetrievedContext(
                        "C1",
                        UUID.randomUUID(),
                        SourceType.EXPERIENCE,
                        UUID.randomUUID(),
                        0,
                        "Built APIs",
                        "Built APIs",
                        0.91,
                        "nomic-embed-text"
                ))
        );

        assertThat(completion.draft().summary().text()).isEqualTo("Builds APIs");
        assertThat(completion.usage().inputTokens()).isZero();
        assertThat(completion.usage().outputTokens()).isZero();
        assertThat(completion.usage().totalTokens()).isZero();
    }

    @Test
    void retriesOnceWhenGeminiReturnsInvalidStructuredJson() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            if (requests.incrementAndGet() == 1) {
                respond(exchange, 200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\"}]}}]}");
                return;
            }
            respond(exchange, 200, responseBody());
        });

        GeminiResumeDraftClient client = new GeminiResumeDraftClient(
                HttpClient.newHttpClient(),
                new GeminiProperties(
                        URI.create("http://localhost:" + server.getAddress().getPort()),
                        "test-api-key",
                        "gemini-test",
                        Duration.ofSeconds(2),
                        1,
                        Duration.ZERO,
                        4096
                ),
                new ResumeDraftProperties("gemini", "resume-draft-v4", 8, 12000, 800, 512, 2),
                new ObjectMapper(),
                new ResumeDraftPrompt("resume-draft-v4", "Use only cited evidence.")
        );

        LlmResumeDraftCompletion completion = client.generateResumeDraft(
                jobPosting(),
                List.of(new RetrievedContext(
                        "C1",
                        UUID.randomUUID(),
                        SourceType.EXPERIENCE,
                        UUID.randomUUID(),
                        0,
                        "Built APIs",
                        "Built APIs",
                        0.91,
                        "nomic-embed-text"
                ))
        );

        assertThat(completion.draft().headline().text()).isEqualTo("Backend Developer");
        assertThat(requests).hasValue(2);
    }

    private JobPosting jobPosting() {
        User user = User.create("gemini@example.com", "hash", "Gemini User", Instant.now());
        return JobPosting.create(
                user,
                "Backend Developer",
                "Example Corp",
                "Build Java APIs.",
                null,
                Instant.now()
        );
    }

    private String responseBody() throws IOException {
        String draft = "{\"headline\":{\"text\":\"Backend Developer\",\"citationIds\":[\"C1\"]},"
                + "\"summary\":{\"text\":\"Builds APIs\",\"citationIds\":[\"C1\"]},"
                + "\"experienceBullets\":[],\"projectBullets\":[],\"skills\":[]}";
        return new ObjectMapper().writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", draft)))
                )),
                "usageMetadata", Map.of(
                        "promptTokenCount", 12,
                        "candidatesTokenCount", 18,
                        "totalTokenCount", 30
                )
        ));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (exchange) {
            exchange.getResponseBody().write(bytes);
        }
    }
}
