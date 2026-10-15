package com.aicareer.platform.tool.application;

public enum ToolErrorCode {
    ARGUMENTS_INVALID("TOOL_ARGUMENTS_INVALID"),
    NOT_FOUND("TOOL_NOT_FOUND"),
    APPROVAL_REQUIRED("TOOL_APPROVAL_REQUIRED"),
    APPROVAL_INVALID("TOOL_APPROVAL_INVALID");

    private final String value;

    ToolErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
