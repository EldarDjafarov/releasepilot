package com.releasepilot.domain.event;

import com.releasepilot.domain.model.Environment;

import java.util.UUID;

public final class PromotionRequestedEvent extends DomainEvent {

    private final String applicationId;
    private final String version;
    private final Environment targetEnvironment;

    public PromotionRequestedEvent(UUID promotionId, String actingUser,
                                   String applicationId, String version,
                                   Environment targetEnvironment) {
        super(promotionId, actingUser);
        this.applicationId = applicationId;
        this.version = version;
        this.targetEnvironment = targetEnvironment;
    }

    public String applicationId() { return applicationId; }
    public String version() { return version; }
    public Environment targetEnvironment() { return targetEnvironment; }

    @Override
    public String eventType() { return "PromotionRequested"; }
}
