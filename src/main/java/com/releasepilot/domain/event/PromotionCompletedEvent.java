package com.releasepilot.domain.event;

import java.util.UUID;

public final class PromotionCompletedEvent extends DomainEvent {

    public PromotionCompletedEvent(UUID promotionId, String actingUser) {
        super(promotionId, actingUser);
    }

    @Override
    public String eventType() { return "PromotionCompleted"; }
}
