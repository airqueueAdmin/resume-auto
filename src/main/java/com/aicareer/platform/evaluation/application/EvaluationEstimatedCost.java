package com.aicareer.platform.evaluation.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationEstimatedCost(
        String currency,
        LocalDate pricingEffectiveFrom,
        BigDecimal inputCost,
        BigDecimal outputCost,
        BigDecimal totalCost
) {
    public EvaluationEstimatedCost {
        if (currency == null || pricingEffectiveFrom == null
                || inputCost == null || outputCost == null || totalCost == null
                || inputCost.signum() < 0 || outputCost.signum() < 0 || totalCost.signum() < 0) {
            throw new IllegalArgumentException("Estimated cost must contain non-negative values and pricing metadata");
        }
    }
}
