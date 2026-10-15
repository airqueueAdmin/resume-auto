package com.aicareer.platform.workflow.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobApplicationContextRequest(
        @NotNull UUID jobPostingId,
        @NotNull @Size(max = 3) List<@NotNull UUID> experienceIds,
        @NotNull @Size(max = 3) List<@NotNull UUID> projectIds
) {
}
