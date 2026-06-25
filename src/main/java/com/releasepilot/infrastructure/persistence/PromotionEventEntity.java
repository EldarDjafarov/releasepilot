package com.releasepilot.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionEventEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "promotion_id", nullable = false, columnDefinition = "uuid")
    private UUID promotionId;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "target_environment", nullable = false)
    private String targetEnvironment;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "acting_user", nullable = false)
    private String actingUser;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason")
    private String reason;
}
