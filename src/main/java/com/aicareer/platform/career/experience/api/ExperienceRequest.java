package com.aicareer.platform.career.experience.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String role,
        LocalDate startDate,
        LocalDate endDate,
        Boolean currentlyEmployed,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 1000) String techStack
) {
}
