package com.aicareer.platform.tool.api;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ToolApprovalRequest(
        @NotBlank String toolName,
        @NotNull Map<String, Object> arguments
) {
}
