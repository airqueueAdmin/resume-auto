package com.aicareer.platform.tool.application;

public class ToolApprovalRequiredException extends ToolException {

    public ToolApprovalRequiredException() {
        super(ToolErrorCode.APPROVAL_REQUIRED, "Explicit approval is required for this tool");
    }
}
