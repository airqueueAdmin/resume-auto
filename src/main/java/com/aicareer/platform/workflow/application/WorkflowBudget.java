package com.aicareer.platform.workflow.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class WorkflowBudget {

    private final Clock clock;
    private final Instant deadline;
    private final int maxSteps;
    private final int maxCostUnits;
    private int stepsUsed;
    private int costUnits;

    public WorkflowBudget(
            Clock clock,
            Duration timeout,
            int maxSteps,
            int maxCostUnits
    ) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Workflow timeout must be positive");
        }
        if (maxSteps < 1 || maxCostUnits < 1) {
            throw new IllegalArgumentException("Workflow limits must be positive");
        }
        this.clock = clock;
        this.deadline = clock.instant().plus(timeout);
        this.maxSteps = maxSteps;
        this.maxCostUnits = maxCostUnits;
    }

    public void beforeStep(int cost) {
        checkDeadline();
        if (cost < 1) {
            throw new IllegalArgumentException("Workflow step cost must be positive");
        }
        if (stepsUsed >= maxSteps) {
            throw new WorkflowLimitExceededException("Workflow step limit exceeded");
        }
        if (costUnits > maxCostUnits - cost) {
            throw new WorkflowLimitExceededException("Workflow cost limit exceeded");
        }
        stepsUsed++;
        costUnits += cost;
    }

    public void afterStep() {
        checkDeadline();
    }

    public int stepsUsed() {
        return stepsUsed;
    }

    public int costUnits() {
        return costUnits;
    }

    private void checkDeadline() {
        if (!clock.instant().isBefore(deadline)) {
            throw new WorkflowTimeoutException();
        }
    }
}
