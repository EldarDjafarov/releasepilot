package com.releasepilot.domain.event;

import java.util.UUID;

public final class DeploymentStartedEvent extends DomainEvent {

    public DeploymentStartedEvent(UUID promotionId, String actingUser) {
        super(promotionId, actingUser);
    }

    @Override
    public String eventType() { return "DeploymentStarted"; }
}
