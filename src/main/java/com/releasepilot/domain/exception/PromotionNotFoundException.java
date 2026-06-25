package com.releasepilot.domain.exception;

public class PromotionNotFoundException extends DomainException {
    public PromotionNotFoundException(String promotionId) {
        super("Promotion '%s' not found".formatted(promotionId));
    }
}
