package com.aicareer.platform.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.aicareer.platform.ai.application.LlmUsage;
import org.junit.jupiter.api.Test;

class EvaluationMetricCalculatorTest {

    private final EvaluationMetricCalculator calculator = new EvaluationMetricCalculator();

    @Test
    void calculatesRetrievalCitationGroundingAndUsageMetrics() {
        EvaluationMetrics metrics = calculator.calculate(new EvaluationObservation(
                Set.of("experience-a", "project-b"),
                List.of("experience-a", "unrelated"),
                List.of("experience-a", "not-retrieved"),
                4,
                3,
                250,
                new LlmUsage(100, 40, 140)
        ));

        assertThat(metrics.retrievalRecallAtK()).isEqualTo(0.5);
        assertThat(metrics.retrievalPrecisionAtK()).isEqualTo(0.5);
        assertThat(metrics.citationValidity()).isEqualTo(0.5);
        assertThat(metrics.citationCoverage()).isEqualTo(0.5);
        assertThat(metrics.unsupportedClaimRate()).isEqualTo(0.25);
        assertThat(metrics.latencyMillis()).isEqualTo(250);
        assertThat(metrics.usage().totalTokens()).isEqualTo(140);
    }

    @Test
    void summarizesMetricsWithMacroAveragesAndTokenTotals() {
        EvaluationMetrics first = calculator.calculate(new EvaluationObservation(
                Set.of("source-a"),
                List.of("source-a"),
                List.of("source-a"),
                2,
                2,
                100,
                new LlmUsage(10, 5, 15)
        ));
        EvaluationMetrics second = calculator.calculate(new EvaluationObservation(
                Set.of("source-b"),
                List.of("other"),
                List.of("not-retrieved"),
                2,
                1,
                300,
                new LlmUsage(20, 8, 28)
        ));

        EvaluationReport report = calculator.summarize(List.of(first, second));

        assertThat(report.caseCount()).isEqualTo(2);
        assertThat(report.meanRetrievalRecallAtK()).isEqualTo(0.5);
        assertThat(report.meanCitationValidity()).isEqualTo(0.5);
        assertThat(report.meanUnsupportedClaimRate()).isEqualTo(0.25);
        assertThat(report.meanLatencyMillis()).isEqualTo(200.0);
        assertThat(report.totalInputTokens()).isEqualTo(30);
        assertThat(report.totalOutputTokens()).isEqualTo(13);
        assertThat(report.totalTokens()).isEqualTo(43);
    }

    @Test
    void rejectsInvalidObservationAndEmptyReport() {
        assertThatThrownBy(() -> new EvaluationObservation(
                Set.of("source-a"),
                List.of(),
                List.of(),
                1,
                2,
                10,
                new LlmUsage(1, 1, 2)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> calculator.summarize(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
