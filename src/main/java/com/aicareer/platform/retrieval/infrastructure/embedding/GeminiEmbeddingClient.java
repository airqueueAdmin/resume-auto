package com.aicareer.platform.retrieval.infrastructure.embedding;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import com.aicareer.platform.retrieval.application.EmbeddingClient;
import com.aicareer.platform.retrieval.application.EmbeddingIntegrationException;
import com.aicareer.platform.retrieval.infrastructure.config.GeminiEmbeddingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final String QUERY_PREFIX = "search_query: ";
    private static final String DOCUMENT_PREFIX = "search_document: ";
    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);

    private final HttpClient httpClient;
    private final GeminiEmbeddingProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingClient(
            HttpClient httpClient,
            GeminiEmbeddingProperties properties,
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
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw EmbeddingIntegrationException.requestFailed(
                    "Gemini embedding API key is not configured"
            );
        }

        String requestBody = createRequestBody(texts);
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                HttpResponse<String> response = send(requestBody);
                if (response.statusCode() == 200) {
                    return parseResponse(response.body(), texts.size());
                }
                log.warn(
                        "Gemini embedding request failed status={} attempt={} maxAttempts={} model={}",
                        response.statusCode(),
                        attempt,
                        properties.maxAttempts(),
                        properties.model()
                );
                if (isRetryableStatus(response.statusCode()) && attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                if (isRetryableStatus(response.statusCode())) {
                    throw EmbeddingIntegrationException.unavailable(
                            "Gemini embedding request failed after retries"
                    );
                }
                throw EmbeddingIntegrationException.requestFailed(
                        "Gemini rejected the embedding request"
                );
            } catch (HttpTimeoutException exception) {
                if (attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                throw EmbeddingIntegrationException.unavailable(
                        "Gemini embedding request timed out",
                        exception
                );
            } catch (IOException exception) {
                if (attempt < properties.maxAttempts()) {
                    waitBeforeRetry();
                    continue;
                }
                throw EmbeddingIntegrationException.unavailable(
                        "Gemini embedding request could not be completed",
                        exception
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw EmbeddingIntegrationException.unavailable(
                        "Gemini embedding request was interrupted",
                        exception
                );
            }
        }

        throw EmbeddingIntegrationException.unavailable(
                "Gemini embedding request could not be completed"
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

    private HttpResponse<String> send(String requestBody)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(batchEndpoint())
                .timeout(properties.timeout())
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", properties.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI batchEndpoint() {
        String endpoint = properties.endpoint().toString().replaceAll("/+$", "");
        String model = properties.model().startsWith("models/")
                ? properties.model().substring("models/".length())
                : properties.model();
        return URI.create(endpoint + "/models/" + model + ":batchEmbedContents");
    }

    private String createRequestBody(List<String> texts) {
        List<Map<String, Object>> requests = new ArrayList<>(texts.size());
        for (String text : texts) {
            String taskType = "RETRIEVAL_DOCUMENT";
            String content = text;
            if (text.startsWith(QUERY_PREFIX)) {
                taskType = "RETRIEVAL_QUERY";
                content = text.substring(QUERY_PREFIX.length());
            } else if (text.startsWith(DOCUMENT_PREFIX)) {
                content = text.substring(DOCUMENT_PREFIX.length());
            }

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", "models/" + modelName());
            request.put("content", Map.of(
                    "parts", List.of(Map.of("text", content))
            ));
            request.put("taskType", taskType);
            request.put("outputDimensionality", properties.dimensions());
            requests.add(request);
        }
        try {
            return objectMapper.writeValueAsString(Map.of("requests", requests));
        } catch (JsonProcessingException exception) {
            throw EmbeddingIntegrationException.requestFailed(
                    "Could not create Gemini embedding request",
                    exception
            );
        }
    }

    private String modelName() {
        return properties.model().startsWith("models/")
                ? properties.model().substring("models/".length())
                : properties.model();
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
                JsonNode valuesNode = embedding.path("values");
                if (!valuesNode.isArray() || valuesNode.size() != properties.dimensions()) {
                    throw new IllegalArgumentException("embedding dimensions are invalid");
                }
                double[] values = new double[valuesNode.size()];
                for (int index = 0; index < valuesNode.size(); index++) {
                    JsonNode value = valuesNode.get(index);
                    if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
                        throw new IllegalArgumentException("embedding value is invalid");
                    }
                    values[index] = value.doubleValue();
                }
                normalize(values);
                result.add(values);
            }
            return result;
        } catch (Exception exception) {
            throw EmbeddingIntegrationException.invalidResponse(
                    "Gemini returned an invalid embedding response",
                    exception
            );
        }
    }

    private void normalize(double[] values) {
        double squaredLength = 0;
        for (double value : values) {
            squaredLength += value * value;
        }
        if (!Double.isFinite(squaredLength) || squaredLength == 0) {
            throw new IllegalArgumentException("embedding norm is invalid");
        }
        double length = Math.sqrt(squaredLength);
        for (int index = 0; index < values.length; index++) {
            values[index] /= length;
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
