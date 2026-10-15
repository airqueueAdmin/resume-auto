package com.aicareer.platform.jobkorea.export.application;

import java.time.Clock;
import java.util.UUID;

import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.career.resume.domain.ResumeVersion;
import com.aicareer.platform.career.resume.infrastructure.persistence.ResumeVersionRepository;
import com.aicareer.platform.jobkorea.export.domain.JobKoreaExportAudit;
import com.aicareer.platform.jobkorea.export.infrastructure.persistence.JobKoreaExportAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobKoreaExportAuditService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final JobKoreaExportAuditRepository auditRepository;
    private final Clock clock;

    public JobKoreaExportAuditService(
            ResumeVersionRepository resumeVersionRepository,
            JobKoreaExportAuditRepository auditRepository,
            Clock clock
    ) {
        this.resumeVersionRepository = resumeVersionRepository;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional
    public JobKoreaExportAudit record(UUID userId, RecordJobKoreaExportAuditCommand command) {
        ResumeVersion resumeVersion = resumeVersionRepository
                .findByIdAndResume_User_IdAndResume_DeletedAtIsNull(command.resumeVersionId(), userId)
                .orElseThrow(CareerResourceNotFoundException::new);

        return auditRepository.save(JobKoreaExportAudit.create(
                userId,
                resumeVersion,
                command.templateVersion(),
                command.result(),
                command.fileSizeBytes(),
                clock.instant()
        ));
    }
}
