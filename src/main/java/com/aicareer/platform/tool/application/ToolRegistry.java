package com.aicareer.platform.tool.application;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    public static final String GET_EXPERIENCE = "career.experience.get";
    public static final String GET_PROJECT = "career.project.get";
    public static final String GET_JOB_POSTING = "career.job-posting.get";
    public static final String CREATE_EXPERIENCE = "career.experience.create";
    public static final String CREATE_JOB_POSTING = "career.job-posting.create";
    public static final String CREATE_PROJECT = "career.project.create";
    public static final String APPROVE_RESUME_DRAFT = "career.resume-draft.approve";

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Map<String, ToolDefinition> definitions;

    public ToolRegistry(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.definitions = definitions();
    }

    public List<ToolDefinition> list() {
        return definitions.values().stream()
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    public ValidatedToolCall validate(String toolName, JsonNode arguments) {
        ToolDefinition definition = definitions.get(toolName);
        if (definition == null) {
            throw new ToolNotFoundException();
        }
        if (arguments == null || arguments.isNull() || !arguments.isObject()) {
            throw invalid(Map.of("arguments", "must be a JSON object"));
        }

        validateTypedArguments(toolName, arguments);
        return new ValidatedToolCall(definition, canonicalize(arguments));
    }

    public String argumentsHash(JsonNode arguments) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(canonicalize(arguments)));
            return toHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Could not hash tool arguments", exception);
        }
    }

    private void validateTypedArguments(String toolName, JsonNode arguments) {
        switch (toolName) {
            case GET_EXPERIENCE -> validate(arguments, GetExperienceToolArguments.class);
            case GET_PROJECT -> validate(arguments, GetProjectToolArguments.class);
            case GET_JOB_POSTING -> validate(arguments, GetJobPostingToolArguments.class);
            case CREATE_EXPERIENCE -> validate(arguments, CreateExperienceToolArguments.class);
            case CREATE_JOB_POSTING -> validate(arguments, CreateJobPostingToolArguments.class);
            case CREATE_PROJECT -> validate(arguments, CreateProjectToolArguments.class);
            case APPROVE_RESUME_DRAFT -> validate(arguments, ApproveResumeDraftToolArguments.class);
            default -> throw new ToolNotFoundException();
        }
    }

    private <T> void validate(JsonNode arguments, Class<T> type) {
        try {
            T value = objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .forType(type)
                    .readValue(arguments.traverse(objectMapper));
            Map<String, String> errors = validator.validate(value).stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            ConstraintViolation::getMessage,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    ));
            if (!errors.isEmpty()) {
                throw invalid(errors);
            }
        } catch (ToolArgumentValidationException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid(Map.of("arguments", "does not match the tool input schema"));
        }
    }

    private ToolArgumentValidationException invalid(Map<String, String> errors) {
        return new ToolArgumentValidationException(errors);
    }

    private Map<String, ToolDefinition> definitions() {
        Map<String, ToolDefinition> result = new LinkedHashMap<>();
        result.put(GET_EXPERIENCE, new ToolDefinition(
                GET_EXPERIENCE,
                "Read one active Experience owned by the authenticated user",
                false,
                uuidSchema("experienceId")
        ));
        result.put(GET_PROJECT, new ToolDefinition(
                GET_PROJECT,
                "Read one active Project owned by the authenticated user",
                false,
                uuidSchema("projectId")
        ));
        result.put(GET_JOB_POSTING, new ToolDefinition(
                GET_JOB_POSTING,
                "Read one active JobPosting owned by the authenticated user",
                false,
                uuidSchema("jobPostingId")
        ));
        result.put(CREATE_EXPERIENCE, new ToolDefinition(
                CREATE_EXPERIENCE,
                "Create an Experience owned by the authenticated user",
                true,
                objectSchema(
                        List.of("companyName", "role", "description"),
                        Map.of(
                                "companyName", stringProperty(200),
                                "role", stringProperty(200),
                                "description", stringProperty(10_000)
                        )
                )
        ));
        result.put(CREATE_JOB_POSTING, new ToolDefinition(
                CREATE_JOB_POSTING,
                "Create a JobPosting owned by the authenticated user",
                true,
                objectSchema(
                        List.of("title", "companyName", "description"),
                        Map.of(
                                "title", stringProperty(200),
                                "companyName", stringProperty(200),
                                "description", stringProperty(100_000),
                                "sourceUrl", Map.of(
                                        "type", "string",
                                        "maxLength", 2_048,
                                        "pattern", "^$|https?://.+"
                                )
                        )
                )
        ));
        result.put(CREATE_PROJECT, new ToolDefinition(
                CREATE_PROJECT,
                "Create a Project owned by the authenticated user",
                true,
                objectSchema(
                        List.of("name", "role", "startDate", "description", "techStack"),
                        Map.of(
                                "name", stringProperty(200),
                                "role", stringProperty(200),
                                "startDate", Map.of("type", "string", "format", "date"),
                                "endDate", Map.of("type", "string", "format", "date"),
                                "description", stringProperty(10_000),
                                "techStack", stringProperty(1_000),
                                "repositoryUrl", Map.of(
                                        "type", "string",
                                        "maxLength", 2_048,
                                        "pattern", "^$|https?://.+"
                                )
                        )
                )
        ));
        result.put(APPROVE_RESUME_DRAFT, new ToolDefinition(
                APPROVE_RESUME_DRAFT,
                "Approve an owned grounded resume draft into a new immutable ResumeVersion",
                true,
                objectSchema(
                        List.of("draftId", "resumeId"),
                        Map.of(
                                "draftId", uuidProperty(),
                                "resumeId", uuidProperty()
                        )
                )
        ));
        return Map.copyOf(result);
    }

    private Map<String, Object> uuidSchema(String requiredProperty) {
        return objectSchema(List.of(requiredProperty), Map.of(requiredProperty, uuidProperty()));
    }

    private Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.copyOf(required));
        schema.put("properties", Map.copyOf(properties));
        return Map.copyOf(schema);
    }

    private Map<String, String> uuidProperty() {
        return Map.of("type", "string", "format", "uuid");
    }

    private Map<String, Object> stringProperty(int maxLength) {
        return Map.of("type", "string", "maxLength", maxLength);
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            node.forEach(value -> result.add(canonicalize(value)));
            return result;
        }
        ObjectNode result = objectMapper.createObjectNode();
        TreeSet<String> fieldNames = new TreeSet<>();
        node.fieldNames().forEachRemaining(fieldNames::add);
        for (String fieldName : fieldNames) {
            result.set(fieldName, canonicalize(node.get(fieldName)));
        }
        return result;
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }
}
