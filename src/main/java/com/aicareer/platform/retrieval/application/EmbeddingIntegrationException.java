package com.aicareer.platform.retrieval.application;

public class EmbeddingIntegrationException extends RuntimeException {

    public enum Kind {
        UNAVAILABLE,
        REQUEST_FAILED,
        INVALID_RESPONSE
    }

    private final Kind kind;

    private EmbeddingIntegrationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public static EmbeddingIntegrationException unavailable(String message) {
        return unavailable(message, null);
    }

    public static EmbeddingIntegrationException unavailable(String message, Throwable cause) {
        return new EmbeddingIntegrationException(Kind.UNAVAILABLE, message, cause);
    }

    public static EmbeddingIntegrationException requestFailed(String message) {
        return requestFailed(message, null);
    }

    public static EmbeddingIntegrationException requestFailed(String message, Throwable cause) {
        return new EmbeddingIntegrationException(Kind.REQUEST_FAILED, message, cause);
    }

    public static EmbeddingIntegrationException invalidResponse(String message, Throwable cause) {
        return new EmbeddingIntegrationException(Kind.INVALID_RESPONSE, message, cause);
    }

    public Kind kind() {
        return kind;
    }
}
