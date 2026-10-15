package com.aicareer.platform.shared.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aicareer.platform.ai.application.AiIntegrationException;
import com.aicareer.platform.ai.application.GroundingValidationException;
import com.aicareer.platform.career.application.CareerResourceNotFoundException;
import com.aicareer.platform.retrieval.application.EmbeddingIntegrationException;
import com.aicareer.platform.tool.application.ToolApprovalInvalidException;
import com.aicareer.platform.tool.application.ToolApprovalRequiredException;
import com.aicareer.platform.tool.application.ToolArgumentValidationException;
import com.aicareer.platform.tool.application.ToolNotFoundException;
import com.aicareer.platform.workflow.application.WorkflowArgumentValidationException;
import com.aicareer.platform.workflow.application.WorkflowLimitExceededException;
import com.aicareer.platform.workflow.application.WorkflowTimeoutException;
import com.aicareer.platform.shared.ratelimit.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import com.aicareer.platform.identity.application.EmailAlreadyRegisteredException;
import com.aicareer.platform.identity.application.InvalidCredentialsException;
import com.aicareer.platform.identity.application.InvalidRefreshTokenException;
import com.aicareer.platform.identity.application.UserNotFoundException;
import com.aicareer.platform.job.application.JobPostingImportException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(
            RateLimitExceededException exception,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        response.setHeader("Retry-After", Long.toString(exception.retryAfterSeconds()));
        return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                "The request rate limit was exceeded",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleAiIntegration(
            AiIntegrationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception.kind() == AiIntegrationException.Kind.UNAVAILABLE
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;
        String code = switch (exception.kind()) {
            case UNAVAILABLE -> "AI_PROVIDER_UNAVAILABLE";
            case REQUEST_FAILED -> "AI_PROVIDER_REQUEST_FAILED";
            case INVALID_RESPONSE -> "AI_PROVIDER_INVALID_RESPONSE";
        };
        String detail = switch (exception.kind()) {
            case UNAVAILABLE -> "The AI provider is temporarily unavailable";
            case REQUEST_FAILED -> "The AI provider rejected the analysis request";
            case INVALID_RESPONSE -> "The AI provider returned an invalid analysis response";
        };
        return error(status, code, detail, request, Map.of());
    }

    @ExceptionHandler(EmbeddingIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleEmbeddingIntegration(
            EmbeddingIntegrationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception.kind() == EmbeddingIntegrationException.Kind.UNAVAILABLE
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;
        String code = switch (exception.kind()) {
            case UNAVAILABLE -> "EMBEDDING_PROVIDER_UNAVAILABLE";
            case REQUEST_FAILED -> "EMBEDDING_PROVIDER_REQUEST_FAILED";
            case INVALID_RESPONSE -> "EMBEDDING_PROVIDER_INVALID_RESPONSE";
        };
        String detail = switch (exception.kind()) {
            case UNAVAILABLE -> "The embedding provider is temporarily unavailable";
            case REQUEST_FAILED -> "The embedding provider rejected the request";
            case INVALID_RESPONSE -> "The embedding provider returned an invalid response";
        };
        return error(status, code, detail, request, Map.of());
    }

    @ExceptionHandler(GroundingValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleGroundingValidation(
            GroundingValidationException exception,
            HttpServletRequest request
    ) {
        String code = switch (exception.kind()) {
            case EMPTY_CONTEXT -> "RAG_CONTEXT_EMPTY";
            case UNKNOWN_CITATION -> "RAG_CITATION_INVALID";
            case INVALID_FINDING -> "RAG_FINDING_INVALID";
            case UNCITED_FINDING -> "RAG_FINDING_UNCITED";
        };
        String detail = switch (exception.kind()) {
            case EMPTY_CONTEXT -> "No owned career evidence was retrieved for the job posting";
            case UNKNOWN_CITATION -> "The AI response contained an unrecognized source citation";
            case INVALID_FINDING -> "The AI response contained an invalid grounded finding";
            case UNCITED_FINDING -> "The AI response contained a finding without supporting citations";
        };
        return error(HttpStatus.UNPROCESSABLE_ENTITY, code, detail, request, Map.of());
    }

    @ExceptionHandler(ToolArgumentValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleToolArgumentValidation(
            ToolArgumentValidationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                exception.errorCode().value(),
                "Tool arguments do not match the published input schema",
                request,
                exception.fieldErrors()
        );
    }

    @ExceptionHandler(ToolNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleToolNotFound(
            ToolNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                exception.errorCode().value(),
                "The requested tool is not allowlisted",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(ToolApprovalRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleToolApprovalRequired(
            ToolApprovalRequiredException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                exception.errorCode().value(),
                "Explicit user approval is required before executing this mutating tool",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(ToolApprovalInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleToolApprovalInvalid(
            ToolApprovalInvalidException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                exception.errorCode().value(),
                "The tool approval is invalid, expired, or already used",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(WorkflowArgumentValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkflowArgumentValidation(
            WorkflowArgumentValidationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                exception.errorCode().value(),
                "Workflow arguments are invalid",
                request,
                exception.fieldErrors()
        );
    }

    @ExceptionHandler(WorkflowLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkflowLimitExceeded(
            WorkflowLimitExceededException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.errorCode().value(),
                "The workflow exceeded its configured step or cost limit",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(WorkflowTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkflowTimeout(
            WorkflowTimeoutException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.GATEWAY_TIMEOUT,
                exception.errorCode().value(),
                "The workflow exceeded its execution deadline",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(JobPostingImportException.class)
    public ResponseEntity<ApiErrorResponse> handleJobPostingImport(
            JobPostingImportException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "JOB_POSTING_IMPORT_FAILED",
                exception.getMessage() == null ? "Could not import the job posting" : exception.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return error(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Request constraint validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body could not be parsed",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "An account with this email already exists",
                request,
                Map.of()
        );
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested user was not found",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(CareerResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCareerResourceNotFound(
            CareerResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "A request parameter has an invalid format",
                request,
                Map.of(exception.getName(), "Invalid format")
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "The request conflicts with existing data",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "The requested method is not supported for this resource",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                exception.getMessage() == null ? "Invalid request" : exception.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled request failure for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        String requestId = requestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                request.getRequestURI(),
                Instant.now(),
                code,
                requestId,
                fieldErrors
        );

        return ResponseEntity.status(status).body(response);
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            return requestId.toString();
        }

        String headerValue = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        return headerValue == null || headerValue.isBlank() ? UUID.randomUUID().toString() : headerValue;
    }
}
