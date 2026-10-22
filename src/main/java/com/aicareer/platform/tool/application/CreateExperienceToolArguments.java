package com.aicareer.platform.tool.application;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExperienceToolArguments(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String role,
        LocalDate startDate,
        LocalDate endDate,
        Boolean currentlyEmployed,
        @NotBlank @Size(max = 10_000) String description
) {
}
