package com.aicareer.platform.evaluation.application;

import com.aicareer.platform.ai.application.LlmUsage;

public record EvaluationMetrics(
        double retrievalRecallAtK,
        double retrievalPrecisionAtK,
        double citationValidity,
        double citationCoverage,
        double unsupportedClaimRate,
        long latencyMillis,
        LlmUsage usage
) {
    public EvaluationMetrics {
        validateRate(retrievalRecallAtK, "retrievalRecallAtK");
        validateRate(retrievalPrecisionAtK, "retrievalPrecisionAtK");
        validateRate(citationValidity, "citationValidity");
        validateRate(citationCoverage, "citationCoverage");
        validateRate(unsupportedClaimRate, "unsupportedClaimRate");
        if (latencyMillis < 0 || usage == null) {
            throw new IllegalArgumentException("Latency and token usage must be valid");
        }
    }

    private static void validateRate(double value, String fieldName) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between zero and one");
        }
    }
}
