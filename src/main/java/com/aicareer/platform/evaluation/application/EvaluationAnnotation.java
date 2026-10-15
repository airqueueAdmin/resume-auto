package com.aicareer.platform.evaluation.application;

public record EvaluationAnnotation(
        String caseId,
        String annotationVersion,
        String reviewer,
        String reviewedAt,
        int claimCount,
        int supportedClaimCount,
        String notes
) {
    public EvaluationAnnotation {
        requireText(caseId, "caseId");
        requireText(annotationVersion, "annotationVersion");
        requireText(reviewer, "reviewer");
        requireText(reviewedAt, "reviewedAt");
        if (claimCount < 0 || supportedClaimCount < 0 || supportedClaimCount > claimCount) {
            throw new IllegalArgumentException("Annotation claim counts are invalid");
        }
        notes = notes == null ? "" : notes;
    }

    public EvaluationObservation applyTo(EvaluationObservation observation) {
        if (observation == null) {
            throw new IllegalArgumentException("Evaluation observation must not be null");
        }
        if (observation.claimCount() != claimCount) {
            throw new IllegalArgumentException("Annotation claim count does not match observation");
        }
        return new EvaluationObservation(
                observation.expectedSourceIds(),
                observation.retrievedSourceIds(),
                observation.citedSourceIds(),
                claimCount,
                supportedClaimCount,
                observation.latencyMillis(),
                observation.usage()
        );
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
