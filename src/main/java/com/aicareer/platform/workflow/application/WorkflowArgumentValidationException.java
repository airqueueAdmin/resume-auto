package com.aicareer.platform.workflow.application;

import java.util.Map;

public class WorkflowArgumentValidationException extends WorkflowException {

    private final Map<String, String> fieldErrors;

    public WorkflowArgumentValidationException(Map<String, String> fieldErrors) {
        super(WorkflowErrorCode.ARGUMENTS_INVALID, "Workflow arguments are invalid");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
