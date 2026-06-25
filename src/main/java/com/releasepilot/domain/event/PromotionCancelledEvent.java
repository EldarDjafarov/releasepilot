package com.releasepilot.domain.event;

import java.util.UUID;

public final class PromotionCancelledEvent extends DomainEvent {

    private final String reason;

    public PromotionCancelledEvent(UUID promotionId, String actingUser, String reason) {
        super(promotionId, actingUser);
        this.reason = reason;
    }

    public String reason() { return reason; }

    @Override
    public String eventType() { return "PromotionCancelled"; }
}
