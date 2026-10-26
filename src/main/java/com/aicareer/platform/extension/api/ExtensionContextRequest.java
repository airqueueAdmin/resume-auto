package com.aicareer.platform.extension.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExtensionContextRequest(
        @NotNull UUID resumeVersionId,
        @NotBlank @Size(max = 3000) String question
) {
}
