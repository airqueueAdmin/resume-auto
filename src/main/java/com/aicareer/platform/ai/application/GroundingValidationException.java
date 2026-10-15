package com.aicareer.platform.ai.application;

public class GroundingValidationException extends RuntimeException {

    public enum Kind {
        EMPTY_CONTEXT,
        UNKNOWN_CITATION,
        INVALID_FINDING,
        UNCITED_FINDING
    }

    private final Kind kind;

    private GroundingValidationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public static GroundingValidationException emptyContext() {
        return new GroundingValidationException(
                Kind.EMPTY_CONTEXT,
                "No owned career evidence was retrieved for the job posting"
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
                "The model returned an empty grounded finding"
        );
    }

    public static GroundingValidationException uncitedFinding() {
        return new GroundingValidationException(
                Kind.UNCITED_FINDING,
                "The model returned a finding without supporting citations"
        );
    }

    public Kind kind() {
        return kind;
    }
}
