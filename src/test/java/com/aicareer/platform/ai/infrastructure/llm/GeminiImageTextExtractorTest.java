package com.aicareer.platform.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.aicareer.platform.ai.infrastructure.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeminiImageTextExtractorTest {

    private HttpServer server;
    private List<String> requestBodies;

    @BeforeEach
    void setUp() throws IOException {
        requestBodies = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsInlineImageDataToGeminiAndReturnsTranscribedText() throws Exception {
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            requestBodies.add(readRequest(exchange));
            respond(exchange, 200, """
                    {"candidates":[{"content":{"parts":[{"text":"이런 업무를 해요\\nJava\\nSpring Boot"}]}}]}
                    """);
        });

        GeminiImageTextExtractor extractor = extractor(1);

        String text = extractor.extract(List.of("https://example.com/posting-1.png"));

        assertThat(text).isEqualTo("이런 업무를 해요\nJava\nSpring Boot");
        assertThat(requestBodies.getFirst())
                .contains("inline_data", "image/png", "AQID", "Extract every readable");
    }

    @Test
    void extractsMultipleImagesIndependentlyAndKeepsSuccessfulChunks() throws Exception {
        AtomicInteger requestNumber = new AtomicInteger();
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            requestBodies.add(readRequest(exchange));
            int currentRequest = requestNumber.incrementAndGet();
            if (currentRequest == 1) {
                respond(exchange, 400, "{\"error\":\"invalid image\"}");
            } else {
                respond(exchange, 200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"두 번째 이미지 텍스트\"}]}}]}");
            }
        });

        GeminiImageTextExtractor extractor = extractor(1);

        String text = extractor.extract(List.of(
                "https://example.com/broken.png",
                "https://example.com/readable.png"
        ));

        assertThat(text).isEqualTo("두 번째 이미지 텍스트");
        assertThat(requestNumber).hasValue(2);
        assertThat(requestBodies).hasSize(2);
    }

    private GeminiImageTextExtractor extractor(int maxAttempts) {
        return new GeminiImageTextExtractor(
                HttpClient.newHttpClient(),
                new GeminiProperties(
                        URI.create("http://localhost:" + server.getAddress().getPort()),
                        "test-api-key",
                        "gemini-test",
                        Duration.ofSeconds(2),
                        maxAttempts,
                        Duration.ZERO,
                        4096
                ),
                new ObjectMapper(),
                imageUrl -> new GeminiImageTextExtractor.ImageData("image/png", "AQID")
        );
    }

    private String readRequest(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
