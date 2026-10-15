package com.aicareer.platform.jobkorea.export.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aicareer.platform.jobkorea.export.infrastructure.config.JobKoreaExportAuditProperties;
import com.aicareer.platform.jobkorea.export.infrastructure.persistence.JobKoreaExportAuditRepository;
import org.junit.jupiter.api.Test;

class JobKoreaExportAuditRetentionServiceTest {

    @Test
    void deletesAuditsOlderThanConfiguredRetentionFromCurrentClock() {
        JobKoreaExportAuditRepository repository = org.mockito.Mockito.mock(JobKoreaExportAuditRepository.class);
        Instant now = Instant.parse("2026-10-14T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        JobKoreaExportAuditRetentionService service = new JobKoreaExportAuditRetentionService(
                repository,
                new JobKoreaExportAuditProperties(true, Duration.ofDays(30)),
                clock
        );
        Instant cutoff = now.minus(Duration.ofDays(30));
        when(repository.deleteCreatedBefore(cutoff)).thenReturn(4);

        assertThat(service.deleteExpired()).isEqualTo(4);
        verify(repository).deleteCreatedBefore(cutoff);
    }
}
