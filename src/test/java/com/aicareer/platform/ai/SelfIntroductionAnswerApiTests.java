package com.aicareer.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class SelfIntroductionAnswerApiTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VALID_ANSWER = "{\"sentences\":[{\"text\":\"Java API를 개발했습니다.\",\"citationIds\":[\"C1\"]}]}";
    private static final AtomicReference<String> PRIMARY_MODE = new AtomicReference<>("UNCITED");
    private static final AtomicReference<String> FALLBACK_ANSWER = new AtomicReference<>(VALID_ANSWER);
    private static final AtomicReference<String> PRIMARY_REQUEST = new AtomicReference<>();
    private static final AtomicInteger FALLBACK_REQUESTS = new AtomicInteger();
    private static final HttpServer PROVIDERS = startProviders();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("self_intro_test").withUsername("self_intro_test").withPassword("self_intro_test");

    @LocalServerPort
    private int port;
    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void providers(DynamicPropertyRegistry properties) {
        String base = "http://127.0.0.1:" + PROVIDERS.getAddress().getPort();
        properties.add("app.ai.provider", () -> "gemini");
        properties.add("app.ocr.gemini.endpoint", () -> base + "/gemini");
        properties.add("app.ocr.gemini.api-key", () -> "test-key");
        properties.add("app.ocr.gemini.model", () -> "gemini-test");
        properties.add("app.ai.ollama.endpoint", () -> base + "/api/chat");
        properties.add("app.ai.ollama.model", () -> "test-llama");
        properties.add("app.retrieval.embedding.ollama.endpoint", () -> base + "/api/embed");
        properties.add("app.ai.self-introduction.max-attempts", () -> 1);
    }

    @BeforeEach
    void resetProviders() {
        PRIMARY_MODE.set("UNCITED");
        FALLBACK_ANSWER.set(VALID_ANSWER);
        PRIMARY_REQUEST.set("");
        FALLBACK_REQUESTS.set(0);
    }

    @AfterAll
    static void closeProviders() {
        PROVIDERS.stop(0);
    }

    @Test
    void recoversFromGroundingFailureAndTokenTruncationThroughTheAuthenticatedApi() throws Exception {
        SavedQuestion question = savedQuestion();
        for (String mode : List.of("UNCITED", "MAX_TOKENS")) {
            PRIMARY_MODE.set(mode);
            FALLBACK_REQUESTS.set(0);

            HttpResponse<String> response = generate(question);

            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            JsonNode draft = MAPPER.readTree(response.body());
            assertThat(draft.path("answer").asText()).isEqualTo("Java API를 개발했습니다 [C1].");
            assertThat(draft.path("provider").asText()).isEqualTo("ollama");
            assertThat(draft.path("model").asText()).isEqualTo("test-llama");
            assertThat(draft.path("promptVersion").asText()).isEqualTo("self-introduction-v5");
            assertThat(draft.path("diagnosticsVisible").asBoolean(true)).isFalse();
            assertThat(draft.path("citations").size()).isEqualTo(1);
            assertThat(draft.path("citations").get(0).path("content").asText()).contains("Java API");
            assertThat(draft.path("retrievedChunkCount").asInt()).isEqualTo(2);
            assertThat(FALLBACK_REQUESTS).hasValue(1);
            assertThat(PRIMARY_REQUEST.get()).contains("Example Corp", "직무 관련 경험", "EXPERIENCE", "PROJECT");
        }
        JsonNode saved = MAPPER.readTree(request("GET", question.path(), null, question.token()).body());
        assertThat(saved.path("content").get(0).path("answer").asText()).isEmpty();
    }

    @Test
    void rejectsUncitedFallbackOutputAndKeepsTheSavedQuestion() throws Exception {
        SavedQuestion question = savedQuestion();
        FALLBACK_ANSWER.set("{\"sentences\":[{\"text\":\"근거 없는 성과입니다.\",\"citationIds\":[\"C99\"]}]}");

        HttpResponse<String> response = generate(question);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("RAG_CITATION_INVALID").doesNotContain("근거 없는 성과");
        assertThat(FALLBACK_REQUESTS).hasValue(1);
        assertThat(request("GET", question.path(), null, question.token()).statusCode()).isEqualTo(200);
    }

    @Test
    void distinguishesModelAbstentionFromAnEmptySearchIndex() throws Exception {
        SavedQuestion question = savedQuestion();
        PRIMARY_MODE.set("UNSUPPORTED");

        HttpResponse<String> response = generate(question);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("RAG_ANSWER_UNSUPPORTED").doesNotContain("RAG_CONTEXT_EMPTY");
        assertThat(FALLBACK_REQUESTS).hasValue(0);
    }

    private SavedQuestion savedQuestion() throws Exception {
        String email = "self-intro-" + UUID.randomUUID() + "@example.com";
        Map<String, String> account = Map.of("email", email, "password", "Test-password-1234!", "displayName", "Test User");
        post("/api/v1/auth/signup", account, null, 201);
        String token = post("/api/v1/auth/login", account, null, 200).path("accessToken").asText();
        post("/api/v1/experiences", Map.of("companyName", "Career Corp", "role", "Backend Engineer",
                "description", "Java API를 개발했습니다."), token, 201);
        post("/api/v1/projects", Map.of("name", "API Project", "role", "Backend Engineer", "startDate", "2025-01-01",
                "description", "Java API를 개발했습니다.", "techStack", "Java, PostgreSQL"), token, 201);
        post("/api/v1/retrieval/index", Map.of(), token, 200);
        String resumeId = post("/api/v1/resumes", Map.of("name", "Application Resume", "content", "Reviewed resume"), token, 201)
                .path("id").asText();
        String postingId = post("/api/v1/job-postings", Map.of("title", "Backend Engineer", "companyName", "Example Corp",
                "description", "Java API 개발 경험이 필요합니다."), token, 201).path("id").asText();
        String path = "/api/v1/resumes/" + resumeId + "/application-questions";
        String questionId = post(path, Map.of("jobPostingId", postingId, "question", "직무 관련 경험을 작성해주세요.", "answer", ""), token, 201)
                .path("id").asText();
        return new SavedQuestion(token, resumeId, questionId, path);
    }

    private HttpResponse<String> generate(SavedQuestion question) throws Exception {
        return request("POST", "/api/v1/ai/generate-self-introduction-answer",
                MAPPER.writeValueAsString(Map.of("resumeId", question.resumeId(), "questionId", question.questionId())), question.token());
    }

    private JsonNode post(String path, Object body, String token, int status) throws Exception {
        HttpResponse<String> response = request("POST", path, MAPPER.writeValueAsString(body), token);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return MAPPER.readTree(response.body());
    }

    private HttpResponse<String> request(String method, String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer startProviders() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/gemini/models/gemini-test:generateContent", exchange -> {
                PRIMARY_REQUEST.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String mode = PRIMARY_MODE.get();
                String answer = mode.equals("UNSUPPORTED") ? "{\"sentences\":[]}"
                        : mode.equals("MAX_TOKENS") ? VALID_ANSWER
                        : "{\"sentences\":[{\"text\":\"Java API를 개발했습니다.\",\"citationIds\":[]}]}";
                respond(exchange, Map.of("candidates", List.of(Map.of("finishReason", mode.equals("MAX_TOKENS") ? "MAX_TOKENS" : "STOP",
                        "content", Map.of("parts", List.of(Map.of("text", answer)))))));
            });
            server.createContext("/api/chat", exchange -> {
                FALLBACK_REQUESTS.incrementAndGet();
                respond(exchange, Map.of("message", Map.of("content", FALLBACK_ANSWER.get()), "done", true,
                        "done_reason", "stop", "prompt_eval_count", 10, "eval_count", 20));
            });
            server.createContext("/api/embed", exchange -> {
                JsonNode body = MAPPER.readTree(exchange.getRequestBody());
                List<Double> vector = new java.util.ArrayList<>(Collections.nCopies(768, 0.0));
                vector.set(0, 1.0);
                respond(exchange, Map.of("embeddings", Collections.nCopies(body.path("input").size(), vector)));
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot start test provider endpoints", error);
        }
    }

    private static void respond(HttpExchange exchange, Object response) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (exchange) { exchange.getResponseBody().write(bytes); }
    }

    private record SavedQuestion(String token, String resumeId, String questionId, String path) {
    }
}
