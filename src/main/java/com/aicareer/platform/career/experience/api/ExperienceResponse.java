package com.aicareer.platform.career.experience.api;

import java.time.Instant;
import java.util.UUID;

import com.aicareer.platform.career.experience.domain.Experience;

public record ExperienceResponse(
        UUID id,
        String companyName,
        String role,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static ExperienceResponse from(Experience experience) {
        return new ExperienceResponse(
                experience.getId(),
                experience.getCompanyName(),
                experience.getRole(),
                experience.getDescription(),
                experience.getCreatedAt(),
                experience.getUpdatedAt()
        );
    }
}
