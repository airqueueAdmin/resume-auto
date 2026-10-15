package com.aicareer.platform.job.application;

public class JobPostingImportException extends RuntimeException {

    public JobPostingImportException(String message) {
        super(message);
    }

    public JobPostingImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
