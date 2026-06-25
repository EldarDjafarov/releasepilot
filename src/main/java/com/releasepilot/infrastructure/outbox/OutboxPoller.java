package com.releasepilot.infrastructure.outbox;

import com.releasepilot.infrastructure.messaging.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox table for PENDING events and publishes them to RabbitMQ.
 *
 * Runs every 1 second. In a multi-instance deployment, add SELECT FOR UPDATE SKIP LOCKED
 * to the query to prevent two instances from processing the same event.
 *
 * Guarantees at-least-once delivery:
 * - If the app crashes after publish but before marking SENT → event is re-published on restart
 * - Consumers must be idempotent (AuditLogConsumer inserts by UUID PK — safe)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEventEntity> pending = outboxEventRepository.findPendingEvents();

        if (pending.isEmpty()) return;

        log.debug("Outbox poller found {} pending events", pending.size());

        for (OutboxEventEntity event : pending) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE,
                        event.getEventType(),  // routing key = event type
                        event.getPayload()
                );

                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.debug("Outbox: published and marked SENT — {} ({})",
                        event.getEventType(), event.getId());

            } catch (Exception e) {
                log.error("Outbox: failed to publish event {} ({}): {}",
                        event.getEventType(), event.getId(), e.getMessage());

                event.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(event);
            }
        }
    }
}
