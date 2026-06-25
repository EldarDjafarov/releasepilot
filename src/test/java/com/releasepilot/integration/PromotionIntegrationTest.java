package com.releasepilot.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasepilot.infrastructure.outbox.OutboxEventRepository;
import com.releasepilot.infrastructure.outbox.OutboxStatus;
import com.releasepilot.infrastructure.persistence.PromotionEventJpaRepository;
import com.releasepilot.infrastructure.persistence.PromotionJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PromotionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("releasepilot")
            .withUsername("releasepilot")
            .withPassword("releasepilot");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired PromotionEventJpaRepository promotionEventRepository;
    @Autowired PromotionJpaRepository promotionRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("Full lifecycle: DEV → APPROVED → IN_PROGRESS → COMPLETED with full history")
    void fullPromotionLifecycle() throws Exception {
        String promotionId = createPromotion("payment-service", "2.1.0", "DEV");
        UUID id = UUID.fromString(promotionId);

        mvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approverId", "alice"))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/promotions/{id}/start-deployment", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("actingUser", "deploy-bot"))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/promotions/{id}/complete", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("actingUser", "deploy-bot", "notes", "All checks passed"))))
                .andExpect(status().isNoContent());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(freshEvents(id)).hasSize(4)
        );

        mvc.perform(get("/promotions/{id}", promotionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completionNotes").value("All checks passed"))
                .andExpect(jsonPath("$.history.length()").value(4));
    }

    @Test
    @DisplayName("Outbox: events published and marked SENT within 5 seconds")
    void outboxEventsArePublishedAfterCommit() throws Exception {
        String promotionId = createPromotion("outbox-test-app", "1.0.0", "DEV");
        UUID id = UUID.fromString(promotionId);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long sentCount = transactionTemplate.execute(tx ->
                    outboxEventRepository.findAll().stream()
                            .filter(e -> e.getPromotionId().equals(id))
                            .filter(e -> e.getStatus() == OutboxStatus.SENT)
                            .count()
            );
            assertThat(sentCount).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    @DisplayName("Audit log consumer persists events from RabbitMQ")
    void auditLogConsumerPersistsEvents() throws Exception {
        String promotionId = createPromotion("audit-test-app", "1.0.0", "DEV");
        UUID id = UUID.fromString(promotionId);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(freshEvents(id)).hasSizeGreaterThanOrEqualTo(1)
        );
    }

    @Test
    @DisplayName("Skipping STAGING returns 422 environment-skipped")
    void skippingEnvironmentReturnsDomainError() throws Exception {
        mvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "applicationId", "skip-test-app",
                                "version", "3.0.0",
                                "targetEnvironment", "PRODUCTION",
                                "requestedBy", "alice"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(
                        "https://releasepilot.internal/errors/environment-skipped"));
    }

    @Test
    @DisplayName("Non-approver gets 403 Forbidden")
    void nonApproverGetsForbidden() throws Exception {
        String promotionId = createPromotion("auth-test-app", "1.0.0", "DEV");

        mvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approverId", "charlie"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(
                        "https://releasepilot.internal/errors/unauthorized-approval"));
    }

    @Test
    @DisplayName("Duplicate active promotion returns 409 Conflict")
    void duplicateActivePromotionReturnsConflict() throws Exception {
        createPromotion("conflict-test-app", "1.0.0", "DEV");

        mvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "applicationId", "conflict-test-app",
                                "version", "1.0.0",
                                "targetEnvironment", "DEV",
                                "requestedBy", "alice"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(
                        "https://releasepilot.internal/errors/promotion-already-in-progress"));
    }

    @Test
    @DisplayName("Application status shows latest per environment")
    void applicationStatusShowsLatestPerEnvironment() throws Exception {
        String devPromotion = createPromotion("status-test-app", "1.0.0", "DEV");

        mvc.perform(post("/promotions/{id}/approve", devPromotion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approverId", "alice"))))
                .andExpect(status().isNoContent());
        mvc.perform(post("/promotions/{id}/start-deployment", devPromotion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("actingUser", "bot"))))
                .andExpect(status().isNoContent());
        mvc.perform(post("/promotions/{id}/complete", devPromotion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("actingUser", "bot", "notes", "done"))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/applications/status-test-app/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("status-test-app"))
                .andExpect(jsonPath("$.environments[0].latestStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("Release notes agent persists notes to promotion after approval")
    void releaseNotesAgentGeneratesNotesOnApproval() throws Exception {
        String promotionId = createPromotion("agent-test-app", "1.0.0", "DEV");
        UUID id = UUID.fromString(promotionId);

        mvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approverId", "alice"))))
                .andExpect(status().isNoContent());

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean hasNotes = transactionTemplate.execute(tx -> {
                entityManager.clear();
                return promotionRepository.findById(id)
                        .map(p -> p.getReleaseNotes() != null && !p.getReleaseNotes().isBlank())
                        .orElse(false);
            });
            assertThat(hasNotes).isTrue();
        });
    }

    @Test
    @DisplayName("Release notes visible in promotion detail API")
    void releaseNotesVisibleInApi() throws Exception {
        String promotionId = createPromotion("api-notes-test", "1.0.0", "DEV");
        UUID id = UUID.fromString(promotionId);

        mvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approverId", "alice"))))
                .andExpect(status().isNoContent());

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean hasNotes = transactionTemplate.execute(tx -> {
                entityManager.clear();
                return promotionRepository.findById(id)
                        .map(p -> p.getReleaseNotes() != null)
                        .orElse(false);
            });
            assertThat(hasNotes).isTrue();
        });

        mvc.perform(get("/promotions/{id}", promotionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseNotes").isNotEmpty())
                .andExpect(jsonPath("$.releaseNotes").value(
                        org.hamcrest.Matchers.containsString("## Release Notes")));
    }

    // -------------------------------------------------------------------------

    private java.util.List<?> freshEvents(UUID promotionId) {
        return transactionTemplate.execute(tx -> {
            entityManager.clear();
            return promotionEventRepository.findByPromotionIdOrderByOccurredAtAsc(promotionId);
        });
    }

    private String createPromotion(String appId, String version, String env) throws Exception {
        return mvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "applicationId", appId,
                                "version", version,
                                "targetEnvironment", env,
                                "requestedBy", "alice"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location")
                .replaceAll(".*/promotions/", "");
    }

    private String json(Map<String, String> map) throws Exception {
        return objectMapper.writeValueAsString(map);
    }
}
