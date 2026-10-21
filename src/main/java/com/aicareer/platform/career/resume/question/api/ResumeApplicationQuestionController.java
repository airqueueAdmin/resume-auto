package com.aicareer.platform.career.resume.question.api;

import java.util.UUID;

import com.aicareer.platform.career.resume.question.application.ResumeApplicationQuestionService;
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
@RequestMapping("/api/v1/resumes/{resumeId}/application-questions")
@Tag(name = "Resume application questions")
@SecurityRequirement(name = "bearerAuth")
public class ResumeApplicationQuestionController {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("createdAt"),
            Sort.Order.asc("id")
    );

    private final ResumeApplicationQuestionService service;

    public ResumeApplicationQuestionController(ResumeApplicationQuestionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Save a company-specific resume question and answer")
    public ResponseEntity<ResumeApplicationQuestionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @Valid @RequestBody ResumeApplicationQuestionRequest request
    ) {
        return ResponseEntity.status(CREATED).body(ResumeApplicationQuestionResponse.from(
                service.create(userId(jwt), resumeId, request)
        ));
    }

    @GetMapping
    public PageResponse<ResumeApplicationQuestionResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                service.list(userId(jwt), resumeId, pageQuery.toPageable(DEFAULT_SORT)),
                ResumeApplicationQuestionResponse::from
        );
    }

    @PutMapping("/{questionId}")
    public ResumeApplicationQuestionResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @PathVariable UUID questionId,
            @Valid @RequestBody ResumeApplicationQuestionRequest request
    ) {
        return ResumeApplicationQuestionResponse.from(
                service.update(userId(jwt), resumeId, questionId, request)
        );
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @PathVariable UUID questionId
    ) {
        service.delete(userId(jwt), resumeId, questionId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
