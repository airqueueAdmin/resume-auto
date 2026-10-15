package com.aicareer.platform.ai.infrastructure.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JobAnalysisSchema {

    private JobAnalysisSchema() {
    }

    static Map<String, Object> definition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", Map.of("type", "string"));
        properties.put("roleFamily", Map.of("type", "string"));
        properties.put("seniority", Map.of("type", "string"));
        properties.put("responsibilities", stringArray());
        properties.put("requiredSkills", stringArray());
        properties.put("preferredSkills", stringArray());
        properties.put("keywords", stringArray());

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of(
                "summary",
                "roleFamily",
                "seniority",
                "responsibilities",
                "requiredSkills",
                "preferredSkills",
                "keywords"
        ));
        return schema;
    }

    private static Map<String, Object> stringArray() {
        return Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
    }
}
