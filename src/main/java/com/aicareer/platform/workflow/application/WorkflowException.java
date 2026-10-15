package com.aicareer.platform.workflow.application;

public abstract class WorkflowException extends RuntimeException {

    private final WorkflowErrorCode errorCode;

    protected WorkflowException(WorkflowErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WorkflowErrorCode errorCode() {
        return errorCode;
    }
}
