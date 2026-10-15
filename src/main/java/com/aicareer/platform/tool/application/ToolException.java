package com.aicareer.platform.tool.application;

public abstract class ToolException extends RuntimeException {

    private final ToolErrorCode errorCode;

    protected ToolException(ToolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ToolErrorCode errorCode() {
        return errorCode;
    }
}
