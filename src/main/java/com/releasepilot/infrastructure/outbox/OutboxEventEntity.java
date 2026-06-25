package com.releasepilot.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "promotion_id", nullable = false, columnDefinition = "uuid")
    private UUID promotionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "acting_user", nullable = false)
    private String actingUser;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
