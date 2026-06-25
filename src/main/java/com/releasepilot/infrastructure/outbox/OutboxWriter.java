package com.releasepilot.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasepilot.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes domain events to the outbox table.
 *
 * Called within the same @Transactional as repository.save(promotion) —
 * so the promotion row and the outbox event are committed atomically.
 * If the transaction rolls back, neither is persisted. No orphaned events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void write(DomainEvent event) {
        try {
            OutboxEventEntity entity = OutboxEventEntity.builder()
                    .id(event.eventId())
                    .promotionId(event.promotionId())
                    .eventType(event.eventType())
                    .actingUser(event.actingUser())
                    .occurredAt(event.occurredAt())
                    .payload(serialize(event))
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(entity);

        } catch (Exception e) {
            log.error("Failed to write event {} to outbox", event.eventType(), e);
            throw new RuntimeException("Outbox write failed", e);
        }
    }

    private String serialize(DomainEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", event.eventId().toString());
            payload.put("eventType", event.eventType());
            payload.put("promotionId", event.promotionId().toString());
            payload.put("actingUser", event.actingUser());
            payload.put("occurredAt", event.occurredAt().toString());

            // Add event-specific fields
            if (event instanceof PromotionRequestedEvent e) {
                payload.put("applicationId", e.applicationId());
                payload.put("version", e.version());
                payload.put("targetEnvironment", e.targetEnvironment().name());
            } else if (event instanceof PromotionRolledBackEvent e) {
                payload.put("reason", e.reason());
            } else if (event instanceof PromotionCancelledEvent e) {
                payload.put("reason", e.reason());
            }

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
