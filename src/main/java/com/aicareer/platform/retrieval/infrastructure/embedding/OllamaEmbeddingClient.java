package com.aicareer.platform.retrieval.infrastructure.embedding;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import com.aicareer.platform.retrieval.application.EmbeddingClient;
import com.aicareer.platform.retrieval.application.EmbeddingIntegrationException;
import com.aicareer.platform.retrieval.infrastructure.config.OllamaEmbeddingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OllamaEmbeddingClient implements EmbeddingClient {

    private final HttpClient httpClient;
    private final OllamaEmbeddingProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaEmbeddingClient(
            HttpClient httpClient,
            OllamaEmbeddingProperties properties,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        String requestId = UUID.randomUUID().toString();
        String requestBody = createRequestBody(texts);
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                HttpResponse<String> response = send(requestBody, requestId);
                if (response.statusCode() == 200) {
                    return parseResponse(response.body(), texts.size());
                }
                if (isRetryableStatus(response.statusCode()) && attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                if (isRetryableStatus(response.statusCode())) {
                    throw EmbeddingIntegrationException.unavailable(
                            "Ollama embedding request failed after retries"
                    );
                }
                throw EmbeddingIntegrationException.requestFailed(
                        "Ollama rejected the embedding request"
                );
            } catch (HttpTimeoutException exception) {
                if (attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                throw EmbeddingIntegrationException.unavailable(
                        "Ollama embedding request timed out",
                        exception
                );
            } catch (IOException exception) {
                if (attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                throw EmbeddingIntegrationException.unavailable(
                        "Ollama embedding request could not be completed",
                        exception
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw EmbeddingIntegrationException.unavailable(
                        "Ollama embedding request was interrupted",
                        exception
                );
            }
        }

        throw EmbeddingIntegrationException.unavailable(
                "Ollama embedding request could not be completed"
        );
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public int dimensions() {
        return properties.dimensions();
    }

    private HttpResponse<String> send(String requestBody, String requestId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(properties.endpoint())
                .timeout(properties.timeout())
                .header("Content-Type", "application/json")
                .header("X-Client-Request-Id", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String createRequestBody(List<String> texts) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", texts);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw EmbeddingIntegrationException.requestFailed(
                    "Could not create Ollama embedding request",
                    exception
            );
        }
    }

    private List<double[]> parseResponse(String body, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode embeddings = root.path("embeddings");
            if (!embeddings.isArray() || embeddings.size() != expectedCount) {
                throw new IllegalArgumentException("embeddings count is invalid");
            }

            List<double[]> result = new ArrayList<>(embeddings.size());
            for (JsonNode embedding : embeddings) {
                if (!embedding.isArray() || embedding.size() != properties.dimensions()) {
                    throw new IllegalArgumentException("embedding dimensions are invalid");
                }
                double[] values = new double[embedding.size()];
                for (int index = 0; index < embedding.size(); index++) {
                    JsonNode value = embedding.get(index);
                    if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
                        throw new IllegalArgumentException("embedding value is invalid");
                    }
                    values[index] = value.doubleValue();
                }
                result.add(values);
            }
            return result;
        } catch (Exception exception) {
            throw EmbeddingIntegrationException.invalidResponse(
                    "Ollama returned an invalid embedding response",
                    exception
            );
        }
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private void waitBeforeRetry() {
        Duration delay = properties.retryDelay();
        if (!delay.isZero() && !delay.isNegative()) {
            LockSupport.parkNanos(delay.toNanos());
        }
    }
}
