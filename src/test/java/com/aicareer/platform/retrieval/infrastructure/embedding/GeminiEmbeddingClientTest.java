package com.aicareer.platform.retrieval.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.aicareer.platform.retrieval.application.EmbeddingIntegrationException;
import com.aicareer.platform.retrieval.infrastructure.config.GeminiEmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingClientTest {

    private HttpServer server;
    private AtomicInteger requests;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> apiKey;

    @BeforeEach
    void setUp() throws IOException {
        requests = new AtomicInteger();
        requestBody = new AtomicReference<>();
        apiKey = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsBatchRequestWithRetrievalTaskTypesAndNormalizesVectors() throws Exception {
        server.createContext("/v1beta/models/test-embedding-model:batchEmbedContents", exchange -> {
            requests.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            respond(exchange, 200, "{\"embeddings\":[{\"values\":[3,4,0]},{\"values\":[0,0,2]}]}");
        });

        var embeddings = client(3, 2, Duration.ZERO).embed(List.of(
                "search_query: target role",
                "search_document: career evidence"
        ));

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).containsExactly(0.6, 0.8, 0.0);
        assertThat(embeddings.get(1)).containsExactly(0.0, 0.0, 1.0);
        assertThat(requests).hasValue(1);
        assertThat(apiKey).hasValue("test-api-key");

        JsonNode body = new ObjectMapper().readTree(requestBody.get());
        assertThat(body.path("requests")).hasSize(2);
        assertThat(body.at("/requests/0/model").asText()).isEqualTo("models/test-embedding-model");
        assertThat(body.at("/requests/0/taskType").asText()).isEqualTo("RETRIEVAL_QUERY");
        assertThat(body.at("/requests/1/taskType").asText()).isEqualTo("RETRIEVAL_DOCUMENT");
        assertThat(body.at("/requests/0/outputDimensionality").asInt()).isEqualTo(3);
        assertThat(body.at("/requests/0/content/parts/0/text").asText()).isEqualTo("target role");
    }

    @Test
    void retriesTransientEmbeddingFailureOnce() throws Exception {
        server.createContext("/v1beta/models/test-embedding-model:batchEmbedContents", exchange -> {
            if (requests.incrementAndGet() == 1) {
                respond(exchange, 503, "{\"error\":\"temporary\"}");
            } else {
                respond(exchange, 200, "{\"embeddings\":[{\"values\":[1,0,0]}]}");
            }
        });

        var embeddings = client(3, 2, Duration.ZERO).embed(List.of("search_document: first"));

        assertThat(embeddings.get(0)).containsExactly(1.0, 0.0, 0.0);
        assertThat(requests).hasValue(2);
    }

    @Test
    void rejectsInvalidDimensionWithoutRetry() throws Exception {
        server.createContext("/v1beta/models/test-embedding-model:batchEmbedContents", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, "{\"embeddings\":[{\"values\":[0.1,0.2]}]}");
        });

        assertThatThrownBy(() -> client(3, 2, Duration.ZERO).embed(List.of("search_document: first")))
                .isInstanceOf(EmbeddingIntegrationException.class)
                .extracting(exception -> ((EmbeddingIntegrationException) exception).kind())
                .isEqualTo(EmbeddingIntegrationException.Kind.INVALID_RESPONSE);
        assertThat(requests).hasValue(1);
    }

    private GeminiEmbeddingClient client(int dimensions, int maxAttempts, Duration retryDelay) {
        GeminiEmbeddingProperties properties = new GeminiEmbeddingProperties(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1beta"),
                "test-api-key",
                "test-embedding-model",
                dimensions,
                Duration.ofSeconds(2),
                maxAttempts,
                retryDelay
        );
        return new GeminiEmbeddingClient(
                HttpClient.newHttpClient(),
                properties,
                new ObjectMapper().findAndRegisterModules()
        );
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
