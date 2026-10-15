package com.aicareer.platform.career.project.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String role,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotBlank @Size(max = 10000) String description,
        @NotBlank @Size(max = 1000) String techStack,
        @Size(max = 300) String customerOrDomain,
        @Size(max = 2048)
        @Pattern(regexp = "^$|https?://.+", message = "repositoryUrl must use http or https")
        String repositoryUrl
) {
}
