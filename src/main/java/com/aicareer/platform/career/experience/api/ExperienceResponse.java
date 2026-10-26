package com.aicareer.platform.career.experience.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.aicareer.platform.career.experience.domain.Experience;

public record ExperienceResponse(
        UUID id,
        String companyName,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentlyEmployed,
        String description,
        String techStack,
        Instant createdAt,
        Instant updatedAt
) {

    public static ExperienceResponse from(Experience experience) {
        return new ExperienceResponse(
                experience.getId(),
                experience.getCompanyName(),
                experience.getRole(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.isCurrentlyEmployed(),
                experience.getDescription(),
                experience.getTechStack(),
                experience.getCreatedAt(),
                experience.getUpdatedAt()
        );
    }
}
