package com.aicareer.platform.job.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JobPostingImportRequest(
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "https?://.+", message = "sourceUrl must use http or https")
        String sourceUrl
) {
}
