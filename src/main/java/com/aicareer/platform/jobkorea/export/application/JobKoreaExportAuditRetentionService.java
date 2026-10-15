package com.aicareer.platform.jobkorea.export.application;

import java.time.Clock;
import java.time.Instant;

import com.aicareer.platform.jobkorea.export.infrastructure.config.JobKoreaExportAuditProperties;
import com.aicareer.platform.jobkorea.export.infrastructure.persistence.JobKoreaExportAuditRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobKoreaExportAuditRetentionService {

    private final JobKoreaExportAuditRepository auditRepository;
    private final JobKoreaExportAuditProperties properties;
    private final Clock clock;

    public JobKoreaExportAuditRetentionService(
            JobKoreaExportAuditRepository auditRepository,
            JobKoreaExportAuditProperties properties,
            Clock clock
    ) {
        this.auditRepository = auditRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${app.jobkorea.export-audit.cleanup-cron:0 0 3 * * *}",
            zone = "UTC"
    )
    @Transactional
    public long deleteExpired() {
        Instant cutoff = clock.instant().minus(properties.retention());
        return auditRepository.deleteCreatedBefore(cutoff);
    }
}
