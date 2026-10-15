package com.aicareer.platform.tool.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    private static jakarta.validation.Validator validator;
    private ToolRegistry toolRegistry;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validator = null;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        toolRegistry = new ToolRegistry(objectMapper, validator);
    }

    @Test
    void onlyAllowlistedToolsCanBeValidated() throws Exception {
        assertThat(toolRegistry.list())
                .extracting(ToolDefinition::name)
                .containsExactly(
                        ToolRegistry.CREATE_EXPERIENCE,
                        ToolRegistry.GET_EXPERIENCE,
                        ToolRegistry.CREATE_JOB_POSTING,
                        ToolRegistry.GET_JOB_POSTING,
                        ToolRegistry.CREATE_PROJECT,
                        ToolRegistry.GET_PROJECT,
                        ToolRegistry.APPROVE_RESUME_DRAFT
                );
        assertThat(toolRegistry.list().stream()
                .filter(ToolDefinition::mutating)
                .map(ToolDefinition::name))
                .containsExactly(
                        ToolRegistry.CREATE_EXPERIENCE,
                        ToolRegistry.CREATE_JOB_POSTING,
                        ToolRegistry.CREATE_PROJECT,
                        ToolRegistry.APPROVE_RESUME_DRAFT
                );

        assertThatThrownBy(() -> toolRegistry.validate(
                "career.user.delete",
                objectMapper.createObjectNode()
        )).isInstanceOf(ToolNotFoundException.class);
    }

    @Test
    void validationRejectsUnknownFieldsAndMalformedIdentifiers() throws Exception {
        UUID experienceId = UUID.randomUUID();
        var unknownField = objectMapper.readTree("""
                {"experienceId":"%s","userId":"%s"}
                """.formatted(experienceId, UUID.randomUUID()));
        var malformedId = objectMapper.readTree("""
                {"experienceId":"not-a-uuid"}
                """);

        assertThatThrownBy(() -> toolRegistry.validate(ToolRegistry.GET_EXPERIENCE, unknownField))
                .isInstanceOf(ToolArgumentValidationException.class);
        assertThatThrownBy(() -> toolRegistry.validate(ToolRegistry.GET_EXPERIENCE, malformedId))
                .isInstanceOf(ToolArgumentValidationException.class);
    }

    @Test
    void argumentHashIsIndependentOfObjectFieldOrder() throws Exception {
        var first = objectMapper.readTree("""
                {"draftId":"%s","resumeId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        var second = objectMapper.readTree("""
                {"resumeId":"%s","draftId":"%s"}
                """.formatted(first.get("resumeId").asText(), first.get("draftId").asText()));

        assertThat(toolRegistry.argumentsHash(first)).isEqualTo(toolRegistry.argumentsHash(second));
    }
}
