package com.aicareer.platform.ai.infrastructure.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SelfIntroductionAnswerSchema {

    private SelfIntroductionAnswerSchema() {
    }

    static Map<String, Object> definition() {
        return definition(12);
    }

    static Map<String, Object> definition(int maximumSentences) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "sentences", Map.of(
                        "type", "array",
                        "minItems", 0,
                        "maxItems", maximumSentences,
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "text", Map.of("type", "string"),
                                        "citationIds", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "maxItems", 5,
                                                "items", Map.of("type", "string")
                                        )
                                ),
                                "required", List.of("text", "citationIds")
                        )
                )
        ));
        schema.put("required", List.of("sentences"));
        return schema;
    }
}
