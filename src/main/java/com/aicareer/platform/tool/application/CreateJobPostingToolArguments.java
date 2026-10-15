package com.aicareer.platform.tool.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateJobPostingToolArguments(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 100_000) String description,
        @Size(max = 2_048)
        @Pattern(regexp = "^$|https?://.+", message = "sourceUrl must use http or https")
        String sourceUrl
) {
}
