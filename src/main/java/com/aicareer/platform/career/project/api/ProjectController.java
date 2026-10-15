package com.aicareer.platform.career.project.api;

import java.util.UUID;

import com.aicareer.platform.career.project.application.ProjectService;
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
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id")
    );

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(ProjectResponse.from(projectService.create(userId(jwt), request)));
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery pageQuery = new PageQuery(page, size);
        return PageResponse.from(
                projectService.list(userId(jwt), pageQuery.toPageable(DEFAULT_SORT)),
                ProjectResponse::from
        );
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId
    ) {
        return ProjectResponse.from(projectService.get(userId(jwt), projectId));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request
    ) {
        return ProjectResponse.from(projectService.update(userId(jwt), projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId
    ) {
        projectService.delete(userId(jwt), projectId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
