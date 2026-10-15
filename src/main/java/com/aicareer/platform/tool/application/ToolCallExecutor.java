package com.aicareer.platform.tool.application;

import java.util.UUID;

/**
 * Application boundary for executing one authenticated, allowlisted tool call.
 *
 * <p>HTTP and future model providers adapt into this contract; neither provider selects services
 * directly.</p>
 */
public interface ToolCallExecutor {

    ToolCallResult execute(UUID userId, ToolCall call);
}
