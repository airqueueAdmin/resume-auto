package com.aicareer.platform.tool.application;

import java.util.Map;

public class ToolArgumentValidationException extends ToolException {

    private final Map<String, String> fieldErrors;

    public ToolArgumentValidationException(Map<String, String> fieldErrors) {
        super(ToolErrorCode.ARGUMENTS_INVALID, "Tool arguments are invalid");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
