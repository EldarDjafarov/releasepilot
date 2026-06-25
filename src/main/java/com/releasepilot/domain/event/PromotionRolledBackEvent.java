package com.releasepilot.domain.event;

import java.util.UUID;

public final class PromotionRolledBackEvent extends DomainEvent {

    private final String reason;

    public PromotionRolledBackEvent(UUID promotionId, String actingUser, String reason) {
        super(promotionId, actingUser);
        this.reason = reason;
    }

    public String reason() { return reason; }

    @Override
    public String eventType() { return "PromotionRolledBack"; }
}
