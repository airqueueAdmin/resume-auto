package com.aicareer.platform.ai.infrastructure.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ResumeDraftSchema {

    private ResumeDraftSchema() {
    }

    static Map<String, Object> definition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("headline", lineSchema());
        properties.put("summary", lineSchema());
        properties.put("selfIntroductionSections", sectionArraySchema());
        properties.put("experienceBullets", lineArraySchema(2));
        properties.put("projectBullets", lineArraySchema(2));
        properties.put("skills", lineArraySchema(4));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(
                "headline",
                "summary",
                "selfIntroductionSections",
                "experienceBullets",
                "projectBullets",
                "skills"
        ));
        return schema;
    }

    private static Map<String, Object> lineSchema() {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("type", "object");
        line.put("properties", Map.of(
                "text", Map.of("type", "string"),
                "citationIds", Map.of(
                        "type", "array",
                        "maxItems", 1,
                        "items", Map.of("type", "string")
                )
        ));
        line.put("required", List.of("text", "citationIds"));
        return line;
    }

    private static Map<String, Object> sectionArraySchema() {
        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("type", "array");
        sections.put("minItems", 3);
        sections.put("maxItems", 3);
        sections.put("items", sectionSchema());
        return sections;
    }

    private static Map<String, Object> sectionSchema() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("type", "object");
        section.put("properties", Map.of(
                "title", Map.of("type", "string"),
                "content", Map.of("type", "string"),
                "citationIds", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "maxItems", 3,
                        "items", Map.of("type", "string")
                )
        ));
        section.put("required", List.of("title", "content", "citationIds"));
        return section;
    }

    private static Map<String, Object> lineArraySchema(int maxItems) {
        Map<String, Object> lines = new LinkedHashMap<>();
        lines.put("type", "array");
        lines.put("maxItems", maxItems);
        lines.put("items", lineSchema());
        return lines;
    }
}
