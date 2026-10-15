package com.aicareer.platform.tool.application;

import java.util.Map;

public record ToolDefinition(
        String name,
        String description,
        boolean mutating,
        Map<String, Object> inputSchema
) {
}
