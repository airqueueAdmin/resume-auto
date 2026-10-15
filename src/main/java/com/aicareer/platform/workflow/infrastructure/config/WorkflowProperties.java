package com.aicareer.platform.workflow.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workflow.job-application-context")
public record WorkflowProperties(
        Duration timeout,
        int maxSteps,
        int maxCostUnits
) {
}
