package com.aicareer.platform.evaluation.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationProviderPricing(
        String provider,
        String model,
        String currency,
        LocalDate effectiveFrom,
        BigDecimal inputCostPerMillionTokens,
        BigDecimal outputCostPerMillionTokens
) {
    public EvaluationProviderPricing {
        requireText(provider, "provider");
        requireText(model, "model");
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be an uppercase ISO-like three-letter code");
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("Pricing effective date must not be null");
        }
        validateCost(inputCostPerMillionTokens, "inputCostPerMillionTokens");
        validateCost(outputCostPerMillionTokens, "outputCostPerMillionTokens");
    }

    public boolean matches(EvaluationExecutionMetadata metadata) {
        return provider.equals(metadata.provider()) && model.equals(metadata.model());
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void validateCost(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
