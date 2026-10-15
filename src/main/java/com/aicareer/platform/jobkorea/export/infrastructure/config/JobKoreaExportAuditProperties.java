package com.aicareer.platform.jobkorea.export.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jobkorea.export-audit")
public record JobKoreaExportAuditProperties(
        boolean retentionCleanupEnabled,
        Duration retention
) {

    public JobKoreaExportAuditProperties {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("JobKorea export-audit retention must be positive");
        }
    }
}
