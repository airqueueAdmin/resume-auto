package com.aicareer.platform.evaluation.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.aicareer.platform.ai.application.LlmUsage;

public record EvaluationObservation(
        Set<String> expectedSourceIds,
        List<String> retrievedSourceIds,
        List<String> citedSourceIds,
        int claimCount,
        int supportedClaimCount,
        long latencyMillis,
        LlmUsage usage
) {
    public EvaluationObservation {
        expectedSourceIds = normalizedIds(expectedSourceIds, "expectedSourceIds");
        retrievedSourceIds = normalizedIds(retrievedSourceIds, "retrievedSourceIds");
        citedSourceIds = normalizedIds(citedSourceIds, "citedSourceIds");
        if (claimCount < 0) {
            throw new IllegalArgumentException("Claim count must not be negative");
        }
        if (supportedClaimCount < 0 || supportedClaimCount > claimCount) {
            throw new IllegalArgumentException("Supported claim count must be between zero and claim count");
        }
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("Latency must not be negative");
        }
        if (usage == null || usage.inputTokens() < 0 || usage.outputTokens() < 0 || usage.totalTokens() < 0) {
            throw new IllegalArgumentException("Token usage must be present and non-negative");
        }
    }

    private static <T extends Iterable<String>> T requireNonNull(T ids, String fieldName) {
        if (ids == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return ids;
    }

    private static Set<String> normalizedIds(Set<String> ids, String fieldName) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : requireNonNull(ids, fieldName)) {
            addId(normalized, id, fieldName);
        }
        return Set.copyOf(normalized);
    }

    private static List<String> normalizedIds(List<String> ids, String fieldName) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : requireNonNull(ids, fieldName)) {
            addId(normalized, id, fieldName);
        }
        return List.copyOf(normalized);
    }

    private static void addId(Set<String> normalized, String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not contain blank identifiers");
        }
        normalized.add(id.trim());
    }
}
