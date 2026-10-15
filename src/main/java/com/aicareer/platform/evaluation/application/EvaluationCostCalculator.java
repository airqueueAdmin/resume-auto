package com.aicareer.platform.evaluation.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EvaluationCostCalculator {

    private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000L);
    private static final int COST_SCALE = 8;

    public EvaluationCostReport calculate(
            EvaluationRunReport report,
            List<EvaluationProviderPricing> pricing
    ) {
        if (report == null || pricing == null || pricing.isEmpty()) {
            throw new IllegalArgumentException("Evaluation report and pricing are required");
        }
        Map<String, EvaluationProviderPricing> pricingByProviderAndModel = pricingByProviderAndModel(pricing);
        List<EvaluationCostResult> results = report.cases().stream()
                .map(caseResult -> estimate(caseResult, pricingByProviderAndModel))
                .toList();
        String currency = results.get(0).cost().currency();
        if (results.stream().anyMatch(result -> !currency.equals(result.cost().currency()))) {
            throw new IllegalArgumentException("A cost report cannot mix currencies");
        }
        BigDecimal total = results.stream()
                .map(result -> result.cost().totalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EvaluationCostReport(report.datasetVersion(), currency, results, total);
    }

    private Map<String, EvaluationProviderPricing> pricingByProviderAndModel(
            List<EvaluationProviderPricing> pricing
    ) {
        Map<String, EvaluationProviderPricing> result = new HashMap<>();
        for (EvaluationProviderPricing item : pricing) {
            String key = key(item.provider(), item.model());
            if (result.putIfAbsent(key, item) != null) {
                throw new IllegalArgumentException("Duplicate provider pricing");
            }
        }
        return result;
    }

    private EvaluationCostResult estimate(
            EvaluationCaseResult caseResult,
            Map<String, EvaluationProviderPricing> pricingByProviderAndModel
    ) {
        EvaluationExecutionMetadata metadata = caseResult.metadata();
        EvaluationProviderPricing providerPricing = pricingByProviderAndModel.get(
                key(metadata.provider(), metadata.model())
        );
        if (providerPricing == null) {
            throw new IllegalArgumentException(
                    "Missing pricing for " + metadata.provider() + "/" + metadata.model()
            );
        }
        long inputTokens = caseResult.metrics().usage().inputTokens();
        long outputTokens = caseResult.metrics().usage().outputTokens();
        BigDecimal inputCost = cost(inputTokens, providerPricing.inputCostPerMillionTokens());
        BigDecimal outputCost = cost(outputTokens, providerPricing.outputCostPerMillionTokens());
        return new EvaluationCostResult(
                caseResult.caseId(),
                metadata,
                new EvaluationEstimatedCost(
                        providerPricing.currency(),
                        providerPricing.effectiveFrom(),
                        inputCost,
                        outputCost,
                        inputCost.add(outputCost).setScale(COST_SCALE, RoundingMode.HALF_UP)
                )
        );
    }

    private BigDecimal cost(long tokens, BigDecimal pricePerMillionTokens) {
        return pricePerMillionTokens
                .multiply(BigDecimal.valueOf(tokens))
                .divide(TOKENS_PER_MILLION, COST_SCALE, RoundingMode.HALF_UP);
    }

    private String key(String provider, String model) {
        return provider + "\u0000" + model;
    }
}
