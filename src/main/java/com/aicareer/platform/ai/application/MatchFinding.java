package com.aicareer.platform.ai.application;

import java.util.List;

public record MatchFinding(
        String statement,
        List<String> citationIds
) {
    public MatchFinding {
        citationIds = List.copyOf(citationIds == null ? List.of() : citationIds);
    }
}
