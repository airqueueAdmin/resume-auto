package com.aicareer.platform.extension.api;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExtensionAnswerRequest(
        @NotNull UUID resumeVersionId,
        @NotNull UUID jobPostingId,
        @NotBlank @Size(max = 3000) String question,
        @Min(1) @Max(100_000) Integer maxLength
) {
}
