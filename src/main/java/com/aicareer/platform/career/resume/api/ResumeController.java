package com.aicareer.platform.career.resume.api;

import java.util.UUID;

import com.aicareer.platform.career.resume.application.ResumeService;
import com.aicareer.platform.career.resume.application.ResumeVersionService;
import com.aicareer.platform.shared.api.PageQuery;
import com.aicareer.platform.shared.api.PageResponse;
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
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes")
@SecurityRequirement(name = "bearerAuth")
public class ResumeController {

    private static final Sort RESUME_SORT = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id")
    );

    private static final Sort VERSION_SORT = Sort.by(
            Sort.Order.desc("versionNumber"),
            Sort.Order.desc("id")
    );

    private final ResumeService resumeService;
    private final ResumeVersionService resumeVersionService;

    public ResumeController(
            ResumeService resumeService,
            ResumeVersionService resumeVersionService
    ) {
        this.resumeService = resumeService;
        this.resumeVersionService = resumeVersionService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateResumeRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(ResumeResponse.from(resumeService.create(userId(jwt), request)));
    }

    @GetMapping
    public PageResponse<ResumeResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                resumeService.list(userId(jwt), pageQuery.toPageable(RESUME_SORT)),
                ResumeResponse::from
        );
    }

    @GetMapping("/{resumeId}")
    public ResumeResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId
    ) {
        return ResumeResponse.from(resumeService.get(userId(jwt), resumeId));
    }

    @PutMapping("/{resumeId}")
    public ResumeResponse rename(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @Valid @RequestBody RenameResumeRequest request
    ) {
        return ResumeResponse.from(resumeService.rename(userId(jwt), resumeId, request));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId
    ) {
        resumeService.delete(userId(jwt), resumeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{resumeId}/versions")
    public ResponseEntity<ResumeVersionResponse> createVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @Valid @RequestBody CreateResumeVersionRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(ResumeVersionResponse.from(
                        resumeVersionService.create(userId(jwt), resumeId, request)
                ));
    }

    @GetMapping("/{resumeId}/versions")
    public PageResponse<ResumeVersionResponse> listVersions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                resumeVersionService.list(userId(jwt), resumeId, pageQuery.toPageable(VERSION_SORT)),
                ResumeVersionResponse::from
        );
    }

    @GetMapping("/{resumeId}/versions/{versionId}")
    public ResumeVersionResponse getVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        return ResumeVersionResponse.from(resumeVersionService.get(userId(jwt), resumeId, versionId));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
