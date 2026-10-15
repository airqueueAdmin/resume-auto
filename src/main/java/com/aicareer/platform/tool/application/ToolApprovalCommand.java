package com.aicareer.platform.tool.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provider-neutral command for issuing a user approval for one exact tool invocation.
 */
public record ToolApprovalCommand(
        String toolName,
        JsonNode arguments
) {
}
