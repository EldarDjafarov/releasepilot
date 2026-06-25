package com.releasepilot.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for all domain events emitted by the Promotion aggregate.
 * Immutable value objects — never mutated after creation.
 */
public abstract sealed class DomainEvent
        permits PromotionRequestedEvent,
                PromotionApprovedEvent,
                DeploymentStartedEvent,
                PromotionCompletedEvent,
                PromotionRolledBackEvent,
                PromotionCancelledEvent {

    private final UUID eventId;
    private final UUID promotionId;
    private final String actingUser;
    private final Instant occurredAt;

    protected DomainEvent(UUID promotionId, String actingUser) {
        this.eventId = UUID.randomUUID();
        this.promotionId = promotionId;
        this.actingUser = actingUser;
        this.occurredAt = Instant.now();
    }

    public UUID eventId() { return eventId; }
    public UUID promotionId() { return promotionId; }
    public String actingUser() { return actingUser; }
    public Instant occurredAt() { return occurredAt; }

    public abstract String eventType();
}
