package com.aicareer.platform.ai.application;

public class AiIntegrationException extends RuntimeException {

    public enum Kind {
        UNAVAILABLE,
        REQUEST_FAILED,
        INVALID_RESPONSE
    }

    private final Kind kind;

    private AiIntegrationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public static AiIntegrationException unavailable(String message) {
        return unavailable(message, null);
    }

    public static AiIntegrationException unavailable(String message, Throwable cause) {
        return new AiIntegrationException(Kind.UNAVAILABLE, message, cause);
    }

    public static AiIntegrationException requestFailed(String message) {
        return requestFailed(message, null);
    }

    public static AiIntegrationException requestFailed(String message, Throwable cause) {
        return new AiIntegrationException(Kind.REQUEST_FAILED, message, cause);
    }

    public static AiIntegrationException invalidResponse(String message, Throwable cause) {
        return new AiIntegrationException(Kind.INVALID_RESPONSE, message, cause);
    }

    public Kind kind() {
        return kind;
    }
}
