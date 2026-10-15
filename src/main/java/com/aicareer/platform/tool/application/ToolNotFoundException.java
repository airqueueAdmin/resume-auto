package com.aicareer.platform.tool.application;

public class ToolNotFoundException extends ToolException {

    public ToolNotFoundException() {
        super(ToolErrorCode.NOT_FOUND, "Unknown tool");
    }
}
