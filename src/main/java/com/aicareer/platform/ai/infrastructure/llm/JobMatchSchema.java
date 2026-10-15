package com.aicareer.platform.ai.infrastructure.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JobMatchSchema {

    private JobMatchSchema() {
    }

    static Map<String, Object> definition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", Map.of("type", "string"));
        properties.put("strengths", findingArray());
        properties.put("gaps", findingArray());
        properties.put("recommendations", findingArray());

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("summary", "strengths", "gaps", "recommendations"));
        return schema;
    }

    private static Map<String, Object> findingArray() {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("type", "object");
        finding.put("additionalProperties", false);
        finding.put("properties", Map.of(
                "statement", Map.of("type", "string"),
                "citationIds", Map.of(
                        "type", "array",
                        "items", Map.of("type", "string")
                )
        ));
        finding.put("required", List.of("statement", "citationIds"));
        return Map.of(
                "type", "array",
                "items", finding
        );
    }
}
