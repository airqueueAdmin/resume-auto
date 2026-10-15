package com.aicareer.platform.ai.infrastructure.llm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aicareer.platform.ai.infrastructure.config.GeminiProperties;
import com.aicareer.platform.job.application.JobPostingImageTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads text embedded in job-posting images through the Gemini API.
 * Each image is an independent request so a slow or invalid image does not discard other results.
 */
public class GeminiImageTextExtractor implements JobPostingImageTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(GeminiImageTextExtractor.class);
    private static final int MAX_IMAGES = 8;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final String OCR_PROMPT = """
            Extract every readable Korean and English character from this job-posting image.
            Return only the extracted posting text. Preserve headings, bullet points, dates,
            URLs, and the original reading order. Do not summarize, interpret, or add commentary.
            If the image is decorative and contains no text, return an empty response.
            """;

    private final HttpClient httpClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final ImageDownloader imageDownloader;

    public GeminiImageTextExtractor(
            HttpClient httpClient,
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this(httpClient, properties, objectMapper,
                imageUrl -> downloadImage(httpClient, properties, imageUrl));
    }

    GeminiImageTextExtractor(
            HttpClient httpClient,
            GeminiProperties properties,
            ObjectMapper objectMapper,
            ImageDownloader imageDownloader
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.imageDownloader = imageDownloader;
    }

    @Override
    public String extract(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()
                || properties.apiKey() == null || properties.apiKey().isBlank()) return "";

        List<String> extractedChunks = new ArrayList<>();
        List<String> limitedImageUrls = imageUrls.stream().limit(MAX_IMAGES).toList();
        for (int imageIndex = 0; imageIndex < limitedImageUrls.size(); imageIndex++) {
            try {
                String extracted = extractOne(limitedImageUrls.get(imageIndex));
                if (!extracted.isBlank()) extractedChunks.add(extracted);
            } catch (Exception exception) {
                // Import preview remains usable when one image times out or is rejected.
                log.warn("Job posting Gemini OCR failed at image {}/{}: {}",
                        imageIndex + 1, limitedImageUrls.size(), exception.toString());
            }
        }
        return clean(String.join("\n\n", extractedChunks));
    }

    private String extractOne(String imageUrl) throws IOException, InterruptedException {
        ImageData image = imageDownloader.download(imageUrl);
        if (image == null || image.base64().isBlank()) return "";

        List<Map<String, Object>> parts = List.of(
                Map.of("text", OCR_PROMPT),
                Map.of("inline_data", Map.of(
                        "mime_type", image.mimeType(),
                        "data", image.base64()
                ))
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", parts
        )));
        body.put("generationConfig", Map.of(
                "temperature", 0.0,
                "maxOutputTokens", Math.max(1000, properties.maxOutputTokens())
        ));

        String requestBody = write(body);
        for (int attempt = 1; attempt <= Math.max(1, properties.maxAttempts()); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(endpoint())
                    .timeout(properties.timeout())
                    .header("x-goog-api-key", properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return clean(responseText(objectMapper.readTree(response.body())));
            }
            if (!isRetryable(response.statusCode()) || attempt == Math.max(1, properties.maxAttempts())) {
                return "";
            }
            waitBeforeRetry();
        }
        return "";
    }

    private URI endpoint() {
        String base = properties.endpoint().toString().replaceAll("/+$", "");
        return URI.create(base + "/models/" + properties.model() + ":generateContent");
    }

    private static ImageData downloadImage(
            HttpClient httpClient,
            GeminiProperties properties,
            String imageUrl
    ) throws IOException, InterruptedException {
        URI uri = URI.create(imageUrl);
        if (!isPublicHttpUri(uri)) return null;
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.timeout())
                .header("User-Agent", "AI-Career-Platform-Job-Importer/1.0")
                .header("Accept", "image/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300
                || response.body().length == 0 || response.body().length > MAX_IMAGE_BYTES) return null;

        String mimeType = response.headers().firstValue("Content-Type")
                .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                .orElse("image/jpeg");
        if (!SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType)) return null;
        return new ImageData(mimeType, Base64.getEncoder().encodeToString(response.body()));
    }

    private static boolean isPublicHttpUri(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || host == null || host.isBlank()) return false;
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) return false;
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) return false;
            }
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private String responseText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode candidate : root.path("candidates")) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                String value = part.path("text").asText("");
                if (!value.isBlank()) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(value);
                }
            }
        }
        return text.toString();
    }

    private String write(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    private boolean isRetryable(int status) {
        return status == 408 || status == 409 || status == 429 || status >= 500;
    }

    private void waitBeforeRetry() throws InterruptedException {
        Duration delay = properties.retryDelay();
        if (!delay.isZero() && !delay.isNegative()) Thread.sleep(delay.toMillis());
    }

    private String clean(String value) {
        return value == null ? "" : value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    @FunctionalInterface
    interface ImageDownloader {
        ImageData download(String imageUrl) throws IOException, InterruptedException;
    }

    record ImageData(String mimeType, String base64) {
    }

    private static final java.util.Set<String> SUPPORTED_IMAGE_MIME_TYPES = java.util.Set.of(
            "image/png", "image/jpeg", "image/webp", "image/heic", "image/heif"
    );
}
