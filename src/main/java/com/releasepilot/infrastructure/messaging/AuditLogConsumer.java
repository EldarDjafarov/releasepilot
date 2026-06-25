package com.releasepilot.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasepilot.infrastructure.persistence.PromotionEventEntity;
import com.releasepilot.infrastructure.persistence.PromotionEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final PromotionEventJpaRepository eventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.AUDIT_QUEUE)
    @Transactional
    public void onEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);

            PromotionEventEntity event = PromotionEventEntity.builder()
                    .id(UUID.fromString(node.get("eventId").asText()))
                    .promotionId(UUID.fromString(node.get("promotionId").asText()))
                    .eventType(node.get("eventType").asText())
                    .actingUser(node.get("actingUser").asText())
                    .occurredAt(Instant.parse(node.get("occurredAt").asText()))
                    .applicationId(node.has("applicationId") ? node.get("applicationId").asText() : "unknown")
                    .targetEnvironment(node.has("targetEnvironment") ? node.get("targetEnvironment").asText() : "unknown")
                    .reason(node.has("reason") ? node.get("reason").asText() : null)
                    .build();

            eventRepository.save(event);

            log.info("Audit: {} for promotion {} by {} at {}",
                    event.getEventType(), event.getPromotionId(),
                    event.getActingUser(), event.getOccurredAt());

        } catch (Exception e) {
            log.error("Failed to persist audit event: {}", payload, e);
            throw new RuntimeException("Audit log processing failed", e);
        }
    }
}
