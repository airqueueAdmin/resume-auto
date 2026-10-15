package com.aicareer.platform.career.project.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.aicareer.platform.career.project.domain.Project;

public record ProjectResponse(
        UUID id,
        String name,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String techStack,
        String repositoryUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRole(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription(),
                project.getTechStack(),
                project.getRepositoryUrl(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
