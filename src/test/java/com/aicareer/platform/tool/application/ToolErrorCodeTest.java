package com.aicareer.platform.tool.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ToolErrorCodeTest {

    @Test
    void applicationFailuresExposeStableProviderNeutralCodes() {
        assertThat(new ToolArgumentValidationException(Map.of()).errorCode().value())
                .isEqualTo("TOOL_ARGUMENTS_INVALID");
        assertThat(new ToolNotFoundException().errorCode().value())
                .isEqualTo("TOOL_NOT_FOUND");
        assertThat(new ToolApprovalRequiredException().errorCode().value())
                .isEqualTo("TOOL_APPROVAL_REQUIRED");
        assertThat(new ToolApprovalInvalidException().errorCode().value())
                .isEqualTo("TOOL_APPROVAL_INVALID");
    }
}
