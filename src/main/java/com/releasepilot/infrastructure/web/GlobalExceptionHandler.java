package com.releasepilot.infrastructure.web;

import com.releasepilot.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice(basePackages = "com.releasepilot.infrastructure.web")
public class GlobalExceptionHandler {

    @ExceptionHandler(EnvironmentSkippedException.class)
    public ProblemDetail handleEnvironmentSkipped(EnvironmentSkippedException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "environment-skipped", ex.getMessage());
    }

    @ExceptionHandler(PromotionAlreadyInProgressException.class)
    public ProblemDetail handleAlreadyInProgress(PromotionAlreadyInProgressException ex) {
        return problem(HttpStatus.CONFLICT, "promotion-already-in-progress", ex.getMessage());
    }

    @ExceptionHandler(InvalidPromotionStateException.class)
    public ProblemDetail handleInvalidState(InvalidPromotionStateException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-promotion-state", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedApprovalException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedApprovalException ex) {
        return problem(HttpStatus.FORBIDDEN, "unauthorized-approval", ex.getMessage());
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    public ProblemDetail handleNotFound(PromotionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "promotion-not-found", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a.isBlank() ? b : a + "; " + b);
        return problem(HttpStatus.BAD_REQUEST, "validation-error", detail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://releasepilot.internal/errors/" + type));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
