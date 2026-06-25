package com.releasepilot.domain.event;

import java.util.UUID;

public final class PromotionApprovedEvent extends DomainEvent {

    public PromotionApprovedEvent(UUID promotionId, String actingUser) {
        super(promotionId, actingUser);
    }

    @Override
    public String eventType() { return "PromotionApproved"; }
}
