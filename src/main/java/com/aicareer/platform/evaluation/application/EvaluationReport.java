package com.aicareer.platform.evaluation.application;

public record EvaluationReport(
        int caseCount,
        double meanRetrievalRecallAtK,
        double meanRetrievalPrecisionAtK,
        double meanCitationValidity,
        double meanCitationCoverage,
        double meanUnsupportedClaimRate,
        double meanLatencyMillis,
        long totalInputTokens,
        long totalOutputTokens,
        long totalTokens
) {
    public EvaluationReport {
        if (caseCount < 1) {
            throw new IllegalArgumentException("Evaluation report must contain at least one case");
        }
    }
}
