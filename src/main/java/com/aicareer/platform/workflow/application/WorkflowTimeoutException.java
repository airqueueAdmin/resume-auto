package com.aicareer.platform.workflow.application;

public class WorkflowTimeoutException extends WorkflowException {

    public WorkflowTimeoutException() {
        super(WorkflowErrorCode.TIMEOUT, "The workflow exceeded its execution deadline");
    }
}
