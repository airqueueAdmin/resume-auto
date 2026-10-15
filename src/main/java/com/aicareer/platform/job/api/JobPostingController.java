package com.aicareer.platform.job.api;

import java.util.UUID;

import com.aicareer.platform.job.application.JobPostingImportService;
import com.aicareer.platform.job.application.JobPostingService;
import com.aicareer.platform.shared.api.PageQuery;
import com.aicareer.platform.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/job-postings")
@Tag(name = "Job Postings")
@SecurityRequirement(name = "bearerAuth")
public class JobPostingController {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id")
    );

    private final JobPostingService jobPostingService;
    private final JobPostingImportService jobPostingImportService;

    public JobPostingController(
            JobPostingService jobPostingService,
            JobPostingImportService jobPostingImportService
    ) {
        this.jobPostingService = jobPostingService;
        this.jobPostingImportService = jobPostingImportService;
    }

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobPostingRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(JobPostingResponse.from(jobPostingService.create(userId(jwt), request)));
    }

    @PostMapping("/import-preview")
    @Operation(summary = "Preview a JobPosting imported from a public URL")
    public JobPostingImportPreview importPreview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobPostingImportRequest request
    ) {
        return jobPostingImportService.preview(request.sourceUrl());
    }

    @GetMapping
    public PageResponse<JobPostingResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                jobPostingService.list(userId(jwt), pageQuery.toPageable(DEFAULT_SORT)),
                JobPostingResponse::from
        );
    }

    @GetMapping("/{jobPostingId}")
    public JobPostingResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobPostingId
    ) {
        return JobPostingResponse.from(jobPostingService.get(userId(jwt), jobPostingId));
    }

    @PutMapping("/{jobPostingId}")
    public JobPostingResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobPostingId,
            @Valid @RequestBody JobPostingRequest request
    ) {
        return JobPostingResponse.from(jobPostingService.update(userId(jwt), jobPostingId, request));
    }

    @DeleteMapping("/{jobPostingId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobPostingId
    ) {
        jobPostingService.delete(userId(jwt), jobPostingId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
