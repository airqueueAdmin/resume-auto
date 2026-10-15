package com.aicareer.platform.tool.api;

import java.time.Instant;

public record ToolApprovalResponse(
        String approvalToken,
        Instant expiresAt
) {
}
