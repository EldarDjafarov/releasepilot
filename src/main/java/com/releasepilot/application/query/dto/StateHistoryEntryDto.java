package com.releasepilot.application.query.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class StateHistoryEntryDto {
    UUID eventId;
    String eventType;
    String actingUser;
    Instant occurredAt;
    String reason;
}
