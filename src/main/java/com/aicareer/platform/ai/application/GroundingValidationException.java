package com.aicareer.platform.ai.application;

public class GroundingValidationException extends RuntimeException {

    public enum Kind {
        EMPTY_CONTEXT,
        UNKNOWN_CITATION,
        INVALID_FINDING,
        UNCITED_FINDING,
        ANSWER_UNSUPPORTED,
        JOB_POSTING_REQUIRED
    }

    private final Kind kind;

    private GroundingValidationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public static GroundingValidationException emptyContext() {
        return new GroundingValidationException(
                Kind.EMPTY_CONTEXT,
                "No sufficiently relevant owned career evidence was found for this application question"
        );
    }

    public static GroundingValidationException unknownCitation(String citationId) {
        return new GroundingValidationException(
                Kind.UNKNOWN_CITATION,
                "The model returned an unknown citation: " + citationId
        );
    }

    public static GroundingValidationException invalidFinding() {
        return new GroundingValidationException(
                Kind.INVALID_FINDING,
                "The model returned an invalid grounded finding"
        );
    }

    public static GroundingValidationException uncitedFinding() {
        return new GroundingValidationException(
                Kind.UNCITED_FINDING,
                "The model returned a finding without supporting citations"
        );
    }

    public static GroundingValidationException answerUnsupported() {
        return new GroundingValidationException(
                Kind.ANSWER_UNSUPPORTED,
                "Career evidence was retrieved, but the model could not support an answer to this question"
        );
    }

    public static GroundingValidationException jobPostingRequired() {
        return new GroundingValidationException(
                Kind.JOB_POSTING_REQUIRED,
                "Link this application question to an active job posting before generating an answer"
        );
    }

    public Kind kind() {
        return kind;
    }
}
