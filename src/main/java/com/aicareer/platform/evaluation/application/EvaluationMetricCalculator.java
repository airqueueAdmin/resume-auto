package com.aicareer.platform.evaluation.application;

import java.util.List;
import java.util.Set;

public final class EvaluationMetricCalculator {

    public EvaluationMetrics calculate(EvaluationObservation observation) {
        if (observation == null) {
            throw new IllegalArgumentException("Evaluation observation must not be null");
        }

        Set<String> expected = observation.expectedSourceIds();
        Set<String> retrieved = Set.copyOf(observation.retrievedSourceIds());
        Set<String> cited = Set.copyOf(observation.citedSourceIds());
        long relevantRetrieved = retrieved.stream().filter(expected::contains).count();
        long validCitations = cited.stream().filter(retrieved::contains).count();
        long expectedCitations = cited.stream().filter(expected::contains).count();

        return new EvaluationMetrics(
                ratio(relevantRetrieved, expected.size()),
                ratio(relevantRetrieved, retrieved.size()),
                ratio(validCitations, cited.size()),
                ratio(expectedCitations, expected.size()),
                ratio(observation.claimCount() - observation.supportedClaimCount(), observation.claimCount()),
                observation.latencyMillis(),
                observation.usage()
        );
    }

    public EvaluationReport summarize(List<EvaluationMetrics> metrics) {
        if (metrics == null || metrics.isEmpty() || metrics.stream().anyMatch(metric -> metric == null)) {
            throw new IllegalArgumentException("Evaluation metrics must contain at least one non-null result");
        }
        double count = metrics.size();
        return new EvaluationReport(
                metrics.size(),
                average(metrics, EvaluationMetrics::retrievalRecallAtK),
                average(metrics, EvaluationMetrics::retrievalPrecisionAtK),
                average(metrics, EvaluationMetrics::citationValidity),
                average(metrics, EvaluationMetrics::citationCoverage),
                average(metrics, EvaluationMetrics::unsupportedClaimRate),
                average(metrics, metric -> metric.latencyMillis()),
                metrics.stream().mapToLong(metric -> metric.usage().inputTokens()).sum(),
                metrics.stream().mapToLong(metric -> metric.usage().outputTokens()).sum(),
                metrics.stream().mapToLong(metric -> metric.usage().totalTokens()).sum()
        );
    }

    private double average(List<EvaluationMetrics> metrics, ToDouble extractor) {
        return metrics.stream().mapToDouble(extractor::value).average().orElseThrow();
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    @FunctionalInterface
    private interface ToDouble {
        double value(EvaluationMetrics metrics);
    }
}
