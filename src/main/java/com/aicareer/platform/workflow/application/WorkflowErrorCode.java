package com.aicareer.platform.workflow.application;

public enum WorkflowErrorCode {
    ARGUMENTS_INVALID("WORKFLOW_ARGUMENTS_INVALID"),
    LIMIT_EXCEEDED("WORKFLOW_LIMIT_EXCEEDED"),
    TIMEOUT("WORKFLOW_TIMEOUT");

    private final String value;

    WorkflowErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
