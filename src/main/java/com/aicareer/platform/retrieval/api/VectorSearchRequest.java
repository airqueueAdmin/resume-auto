package com.aicareer.platform.retrieval.api;

import java.util.Collections;
import java.util.Set;

import com.aicareer.platform.retrieval.domain.SourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VectorSearchRequest(
        @NotBlank(message = "query must not be blank")
        @Size(max = 2000, message = "query must be at most 2000 characters")
        String query,
        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must be at most 20")
        Integer topK,
        @Size(max = 2, message = "sourceTypes must contain at most 2 values")
        Set<SourceType> sourceTypes
) {

    public int effectiveTopK() {
        return topK == null ? 5 : topK;
    }

    public Set<SourceType> effectiveSourceTypes() {
        return sourceTypes == null ? Collections.emptySet() : Set.copyOf(sourceTypes);
    }
}
