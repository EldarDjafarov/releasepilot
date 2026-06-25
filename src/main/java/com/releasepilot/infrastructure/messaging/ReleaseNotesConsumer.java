package com.releasepilot.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasepilot.infrastructure.agent.ReleaseNotesAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes PromotionApproved events and triggers the release notes agent.
 *
 * Runs async — the API has already returned before this executes.
 * The agent itself may take several "LLM iterations" to complete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseNotesConsumer {

    private final ReleaseNotesAgent agent;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.RELEASE_NOTES_QUEUE)
    @Async
    public void onPromotionApproved(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID promotionId = UUID.fromString(node.get("promotionId").asText());

            log.info("Release notes agent triggered for promotion {}", promotionId);
            String notes = agent.generateReleaseNotes(promotionId);
            log.info("Release notes generated for promotion {}: {} chars", promotionId, notes.length());

        } catch (Exception e) {
            log.error("Failed to generate release notes: {}", payload, e);
        }
    }
}
