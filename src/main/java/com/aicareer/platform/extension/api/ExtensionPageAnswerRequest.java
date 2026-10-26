package com.aicareer.platform.extension.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtensionPageAnswerRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String jobTitle,
        @NotBlank @Size(max = 50_000) String jobDescription,
        @NotBlank @Size(max = 2048) String sourceUrl,
        @NotBlank @Size(max = 3000) String question,
        @Min(1) @Max(100_000) Integer maxLength
) {
}
