package com.aicareer.platform.tool.api;

import java.util.Map;

import com.aicareer.platform.tool.application.ToolDefinition;

public record ToolDefinitionResponse(
        String name,
        String description,
        boolean mutating,
        Map<String, Object> inputSchema
) {

    public static ToolDefinitionResponse from(ToolDefinition definition) {
        return new ToolDefinitionResponse(
                definition.name(),
                definition.description(),
                definition.mutating(),
                definition.inputSchema()
        );
    }
}
