package com.aicareer.platform.career.experience.api;

import java.util.UUID;

import com.aicareer.platform.career.experience.application.ExperienceService;
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
@RequestMapping("/api/v1/experiences")
@Tag(name = "Experiences")
@SecurityRequirement(name = "bearerAuth")
public class ExperienceController {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id")
    );

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @PostMapping
    public ResponseEntity<ExperienceResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExperienceRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(ExperienceResponse.from(experienceService.create(userId(jwt), request)));
    }

    @GetMapping
    public PageResponse<ExperienceResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                experienceService.list(userId(jwt), pageQuery.toPageable(DEFAULT_SORT)),
                ExperienceResponse::from
        );
    }

    @GetMapping("/{experienceId}")
    public ExperienceResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID experienceId
    ) {
        return ExperienceResponse.from(experienceService.get(userId(jwt), experienceId));
    }

    @PutMapping("/{experienceId}")
    public ExperienceResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ExperienceRequest request
    ) {
        return ExperienceResponse.from(experienceService.update(userId(jwt), experienceId, request));
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID experienceId
    ) {
        experienceService.delete(userId(jwt), experienceId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
