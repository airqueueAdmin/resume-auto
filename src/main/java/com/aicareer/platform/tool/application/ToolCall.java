package com.aicareer.platform.tool.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provider-neutral representation of one requested tool invocation.
 */
public record ToolCall(
        String toolName,
        JsonNode arguments,
        String approvalToken
) {
}
