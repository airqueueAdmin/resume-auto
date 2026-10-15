package com.aicareer.platform.tool.application;

import com.fasterxml.jackson.databind.JsonNode;

public record ValidatedToolCall(
        ToolDefinition definition,
        JsonNode arguments
) {
}
