package com.aicareer.platform.tool.application;

/**
 * Provider-neutral result of an authorized tool invocation.
 */
public record ToolCallResult(
        String toolName,
        boolean mutating,
        Object result
) {
}
