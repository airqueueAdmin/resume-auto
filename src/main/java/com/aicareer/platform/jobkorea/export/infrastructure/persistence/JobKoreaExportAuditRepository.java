package com.aicareer.platform.jobkorea.export.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportAudit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobKoreaExportAuditRepository extends JpaRepository<JobKoreaExportAudit, UUID> {

    @Modifying
    @Query("delete from JobKoreaExportAudit audit where audit.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
