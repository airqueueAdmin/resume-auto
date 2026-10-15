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
import com.aicareer.platform.retrieval.infrastructure.config.OllamaEmbeddingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OllamaEmbeddingClientTest {

    private HttpServer server;
    private AtomicInteger requests;
    private AtomicReference<String> requestBody;

    @BeforeEach
    void setUp() throws IOException {
        requests = new AtomicInteger();
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsBatchEmbeddingRequestAndParsesVectors() throws Exception {
        server.createContext("/api/embed", exchange -> {
            requests.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"embeddings\":[[0.1,0.2,0.3],[0.4,0.5,0.6]]}");
        });

        var embeddings = client(3, 2, Duration.ZERO).embed(List.of("first", "second"));

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).containsExactly(0.1, 0.2, 0.3);
        assertThat(requests).hasValue(1);
        assertThat(requestBody).hasValueSatisfying(body -> assertThat(body)
                .contains("\"model\":\"test-embedding-model\"", "first", "second"));
    }

    @Test
    void retriesTransientEmbeddingFailureOnce() throws Exception {
        server.createContext("/api/embed", exchange -> {
            if (requests.incrementAndGet() == 1) {
                respond(exchange, 503, "{\"error\":\"temporary\"}");
            } else {
                respond(exchange, 200, "{\"embeddings\":[[0.1,0.2,0.3]]}");
            }
        });

        var embeddings = client(3, 2, Duration.ZERO).embed(List.of("first"));

        assertThat(embeddings.get(0)).containsExactly(0.1, 0.2, 0.3);
        assertThat(requests).hasValue(2);
    }

    @Test
    void rejectsInvalidDimensionWithoutRetry() throws Exception {
        server.createContext("/api/embed", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, "{\"embeddings\":[[0.1,0.2]]}");
        });

        assertThatThrownBy(() -> client(3, 2, Duration.ZERO).embed(List.of("first")))
                .isInstanceOf(EmbeddingIntegrationException.class)
                .extracting(exception -> ((EmbeddingIntegrationException) exception).kind())
                .isEqualTo(EmbeddingIntegrationException.Kind.INVALID_RESPONSE);
        assertThat(requests).hasValue(1);
    }

    private OllamaEmbeddingClient client(int dimensions, int maxAttempts, Duration retryDelay) {
        OllamaEmbeddingProperties properties = new OllamaEmbeddingProperties(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/api/embed"),
                "test-embedding-model",
                dimensions,
                Duration.ofSeconds(2),
                maxAttempts,
                retryDelay
        );
        return new OllamaEmbeddingClient(
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
