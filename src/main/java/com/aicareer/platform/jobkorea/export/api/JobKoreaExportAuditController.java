package com.aicareer.platform.jobkorea.export.api;

import java.util.UUID;

import com.aicareer.platform.jobkorea.export.application.JobKoreaExportAuditService;
import com.aicareer.platform.jobkorea.export.application.RecordJobKoreaExportAuditCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/jobkorea/export-audits")
@Tag(name = "JobKorea Export")
@SecurityRequirement(name = "bearerAuth")
public class JobKoreaExportAuditController {

    private final JobKoreaExportAuditService auditService;

    public JobKoreaExportAuditController(JobKoreaExportAuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    @Operation(summary = "Record a privacy-safe client-side JobKorea export result")
    public ResponseEntity<JobKoreaExportAuditResponse> record(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobKoreaExportAuditRequest request
    ) {
        return ResponseEntity.status(CREATED).body(JobKoreaExportAuditResponse.from(
                auditService.record(
                        UUID.fromString(jwt.getSubject()),
                        new RecordJobKoreaExportAuditCommand(
                                request.resumeVersionId(),
                                request.templateVersion(),
                                request.result(),
                                request.fileSizeBytes()
                        )
                )
        ));
    }
}
