package com.aicareer.platform.workflow.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class WorkflowBudgetTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void rejectsStepAndCostOverflow() {
        WorkflowBudget budget = new WorkflowBudget(
                Clock.fixed(START, ZoneOffset.UTC),
                Duration.ofSeconds(5),
                2,
                2
        );

        budget.beforeStep(1);
        budget.afterStep();
        budget.beforeStep(1);
        budget.afterStep();

        assertThatThrownBy(() -> budget.beforeStep(1))
                .isInstanceOf(WorkflowLimitExceededException.class);
    }

    @Test
    void rejectsExecutionAfterDeadline() {
        AtomicReference<Instant> now = new AtomicReference<>(START);
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        WorkflowBudget budget = new WorkflowBudget(
                clock,
                Duration.ofSeconds(5),
                2,
                2
        );
        now.set(START.plusSeconds(5));

        assertThatThrownBy(() -> budget.beforeStep(1))
                .isInstanceOf(WorkflowTimeoutException.class);
    }
}
