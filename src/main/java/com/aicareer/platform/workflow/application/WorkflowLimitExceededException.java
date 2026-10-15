package com.aicareer.platform.workflow.application;

public class WorkflowLimitExceededException extends WorkflowException {

    public WorkflowLimitExceededException(String message) {
        super(WorkflowErrorCode.LIMIT_EXCEEDED, message);
    }
}
