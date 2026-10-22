package com.aicareer.platform.identity.application;

public class EmailVerificationException extends RuntimeException {

    public enum Kind {
        REQUIRED,
        INVALID_CODE,
        DELIVERY_UNAVAILABLE
    }

    private final Kind kind;

    public EmailVerificationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public EmailVerificationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
