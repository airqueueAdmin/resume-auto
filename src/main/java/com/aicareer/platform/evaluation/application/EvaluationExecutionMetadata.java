package com.aicareer.platform.evaluation.application;

public record EvaluationExecutionMetadata(
        String provider,
        String model,
        String promptVersion
) {
    public EvaluationExecutionMetadata {
        requireText(provider, "provider");
        requireText(model, "model");
        requireText(promptVersion, "promptVersion");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
