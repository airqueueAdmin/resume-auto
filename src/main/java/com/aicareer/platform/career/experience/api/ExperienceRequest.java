package com.aicareer.platform.career.experience.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String role,
        @NotBlank @Size(max = 10000) String description
) {
}
