package com.aicareer.platform.jobkorea.export.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "app.jobkorea.export-audit",
        name = "retention-cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JobKoreaExportSchedulingConfiguration {
}
