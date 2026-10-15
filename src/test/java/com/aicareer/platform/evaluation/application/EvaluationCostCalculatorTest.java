package com.aicareer.platform.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.aicareer.platform.ai.application.LlmUsage;
import org.junit.jupiter.api.Test;

class EvaluationCostCalculatorTest {

    @Test
    void calculatesCaseAndAggregateCostUsingEffectivePricing() {
        EvaluationRunReport report = new EvaluationRunner(new EvaluationMetricCalculator()).run(
                "phase-8-v1",
                List.of(new EvaluationCase(
                        "matching-case",
                        EvaluationTask.MATCHING,
                        "Spring services",
                        3,
                        List.of("source-a"),
                        ""
                )),
                ignored -> new EvaluationCaseExecution(
                        new EvaluationExecutionMetadata("stub", "model", "prompt-v1"),
                        new EvaluationObservation(
                                Set.of("source-a"),
                                List.of("source-a"),
                                List.of("source-a"),
                                1,
                                1,
                                25,
                                new LlmUsage(1_000, 500, 1_500)
                        )
                )
        );

        EvaluationCostReport costs = new EvaluationCostCalculator().calculate(
                report,
                List.of(new EvaluationProviderPricing(
                        "stub",
                        "model",
                        "USD",
                        LocalDate.of(2026, 10, 1),
                        new BigDecimal("2.00"),
                        new BigDecimal("4.00")
                ))
        );

        assertThat(costs.currency()).isEqualTo("USD");
        assertThat(costs.cases()).hasSize(1);
        assertThat(costs.cases().get(0).cost().inputCost())
                .isEqualByComparingTo("0.00200000");
        assertThat(costs.cases().get(0).cost().outputCost())
                .isEqualByComparingTo("0.00200000");
        assertThat(costs.totalCost()).isEqualByComparingTo("0.00400000");
        assertThat(costs.cases().get(0).cost().pricingEffectiveFrom())
                .isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void rejectsMissingPricingDuplicatePricingAndMixedCurrencies() {
        EvaluationRunReport report = reportFor("stub", "model");
        EvaluationCostCalculator calculator = new EvaluationCostCalculator();
        EvaluationProviderPricing pricing = pricing("stub", "model", "USD");

        assertThatThrownBy(() -> calculator.calculate(report, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(report, List.of(pricing, pricing)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");

        EvaluationRunReport mixedReport = new EvaluationRunner(new EvaluationMetricCalculator()).run(
                "phase-8-v1",
                List.of(
                        evaluationCase("one", "stub", "model"),
                        evaluationCase("two", "other", "other-model")
                ),
                evaluationCase -> new EvaluationCaseExecution(
                        new EvaluationExecutionMetadata(
                                evaluationCase.caseId().equals("one") ? "stub" : "other",
                                evaluationCase.caseId().equals("one") ? "model" : "other-model",
                                "prompt-v1"
                        ),
                        observation()
                )
        );
        assertThatThrownBy(() -> calculator.calculate(mixedReport, List.of(
                pricing,
                pricing("other", "other-model", "EUR")
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies");
    }

    private EvaluationRunReport reportFor(String provider, String model) {
        return new EvaluationRunner(new EvaluationMetricCalculator()).run(
                "phase-8-v1",
                List.of(evaluationCase("one", provider, model)),
                ignored -> new EvaluationCaseExecution(
                        new EvaluationExecutionMetadata(provider, model, "prompt-v1"),
                        observation()
                )
        );
    }

    private EvaluationCase evaluationCase(String id, String provider, String model) {
        return new EvaluationCase(id, EvaluationTask.MATCHING, "query", 1, List.of("source"), provider + model);
    }

    private EvaluationObservation observation() {
        return new EvaluationObservation(
                Set.of("source"), List.of("source"), List.of("source"), 1, 1, 1,
                new LlmUsage(1, 1, 2)
        );
    }

    private EvaluationProviderPricing pricing(String provider, String model, String currency) {
        return new EvaluationProviderPricing(
                provider, model, currency, LocalDate.of(2026, 10, 1),
                BigDecimal.ONE, BigDecimal.ONE
        );
    }
}
