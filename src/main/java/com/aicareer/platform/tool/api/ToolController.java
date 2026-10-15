package com.aicareer.platform.tool.api;

import java.util.UUID;

import com.aicareer.platform.tool.application.ToolApprovalService;
import com.aicareer.platform.tool.application.ToolApprovalCommand;
import com.aicareer.platform.tool.application.ToolCall;
import com.aicareer.platform.tool.application.ToolCallExecutor;
import com.aicareer.platform.tool.application.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
@Tag(name = "Tools")
@SecurityRequirement(name = "bearerAuth")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolApprovalService toolApprovalService;
    private final ToolCallExecutor toolCallExecutor;
    private final ObjectMapper objectMapper;

    public ToolController(
            ToolRegistry toolRegistry,
            ToolApprovalService toolApprovalService,
            ToolCallExecutor toolCallExecutor,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.toolApprovalService = toolApprovalService;
        this.toolCallExecutor = toolCallExecutor;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List the allowlisted tools and their input schemas")
    public ToolCatalogResponse list() {
        return new ToolCatalogResponse(toolRegistry.list().stream()
                .map(ToolDefinitionResponse::from)
                .toList());
    }

    @PostMapping("/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue a short-lived one-time approval for a mutating tool call")
    public ToolApprovalResponse issueApproval(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ToolApprovalRequest request
    ) {
        ToolApprovalService.IssuedToolApproval approval = toolApprovalService.issue(
                userId(jwt),
                new ToolApprovalCommand(
                        request.toolName(),
                        objectMapper.valueToTree(request.arguments())
                )
        );
        return new ToolApprovalResponse(approval.token(), approval.expiresAt());
    }

    @PostMapping("/calls")
    @Operation(summary = "Execute one allowlisted tool call in the authenticated user's scope")
    public ToolCallResponse execute(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ToolCallRequest request
    ) {
        return ToolCallResponse.from(toolCallExecutor.execute(
                userId(jwt),
                new ToolCall(
                        request.toolName(),
                        objectMapper.valueToTree(request.arguments()),
                        request.approvalToken()
                )
        ));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
