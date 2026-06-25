package com.releasepilot.domain.exception;

public class PromotionAlreadyInProgressException extends DomainException {
    public PromotionAlreadyInProgressException(String applicationId, String environment) {
        super("A promotion is already in progress for application '%s' in environment '%s'"
                .formatted(applicationId, environment));
    }
}
