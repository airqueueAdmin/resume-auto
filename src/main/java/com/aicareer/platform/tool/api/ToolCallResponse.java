package com.aicareer.platform.tool.api;

import com.aicareer.platform.tool.application.ToolCallResult;

public record ToolCallResponse(
        String toolName,
        boolean mutating,
        Object result
) {

    public static ToolCallResponse from(ToolCallResult result) {
        return new ToolCallResponse(result.toolName(), result.mutating(), result.result());
    }
}
