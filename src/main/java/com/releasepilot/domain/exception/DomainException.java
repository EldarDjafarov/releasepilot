package com.releasepilot.domain.exception;

/**
 * Base class for all domain rule violations.
 * These are NEVER 500s — they map to 4xx in the API layer.
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
