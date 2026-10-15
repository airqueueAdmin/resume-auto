package com.aicareer.platform.tool.application;

public class ToolApprovalInvalidException extends ToolException {

    public ToolApprovalInvalidException() {
        super(ToolErrorCode.APPROVAL_INVALID, "The tool approval is invalid, expired, or already used");
    }
}
