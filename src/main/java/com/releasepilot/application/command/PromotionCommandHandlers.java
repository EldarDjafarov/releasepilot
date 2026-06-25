package com.releasepilot.application.command;

import com.releasepilot.application.command.dto.*;
import com.releasepilot.domain.exception.PromotionAlreadyInProgressException;
import com.releasepilot.domain.exception.PromotionNotFoundException;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.port.DeploymentPort;
import com.releasepilot.domain.port.NotificationPort;
import com.releasepilot.domain.port.PromotionRepository;
import com.releasepilot.domain.service.ApproverService;
import com.releasepilot.infrastructure.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * One handler per command.
 *
 * Each handler:
 *  1. Loads or creates the aggregate
 *  2. Calls the aggregate method (enforces invariants)
 *  3. Saves the aggregate
 *  4. Writes domain events to the outbox — atomically in the same transaction
 *
 * The OutboxPoller picks up events after commit and publishes to RabbitMQ.
 * This guarantees no events are lost even if the broker is temporarily down.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionCommandHandlers {

    private final PromotionRepository repository;
    private final OutboxWriter outboxWriter;
    private final ApproverService approverService;
    private final DeploymentPort deploymentPort;
    private final NotificationPort notificationPort;

    @Transactional
    public UUID handle(RequestPromotion cmd) {
        if (repository.existsActivePromotionFor(cmd.getApplicationId(), cmd.getTargetEnvironment())) {
            throw new PromotionAlreadyInProgressException(
                    cmd.getApplicationId(), cmd.getTargetEnvironment().name());
        }

        List<Environment> completed = repository.findCompletedEnvironmentsFor(
                cmd.getApplicationId(), cmd.getVersion());

        Promotion promotion = Promotion.request(
                cmd.getApplicationId(),
                cmd.getVersion(),
                cmd.getTargetEnvironment(),
                cmd.getRequestedBy(),
                completed
        );

        repository.save(promotion);
        writeToOutbox(promotion);

        log.info("Promotion {} requested: {} v{} → {}",
                promotion.getId(), cmd.getApplicationId(),
                cmd.getVersion(), cmd.getTargetEnvironment());

        return promotion.getId();
    }

    @Transactional
    public void handle(ApprovePromotion cmd) {
        Promotion promotion = load(cmd.getPromotionId());
        promotion.approve(cmd.getApproverId(), approverService.isApprover(cmd.getApproverId()));
        repository.save(promotion);
        writeToOutbox(promotion);
        log.info("Promotion {} approved by {}", cmd.getPromotionId(), cmd.getApproverId());
    }

    @Transactional
    public void handle(StartDeployment cmd) {
        Promotion promotion = load(cmd.getPromotionId());
        promotion.startDeployment(cmd.getActingUser());

        String deploymentId = deploymentPort.triggerDeployment(
                promotion.getId(), promotion.getApplicationId(),
                promotion.getVersion(), promotion.getTargetEnvironment()
        );
        log.info("Deployment triggered for promotion {}: deploymentId={}",
                cmd.getPromotionId(), deploymentId);

        repository.save(promotion);
        writeToOutbox(promotion);
    }

    @Transactional
    public void handle(CompletePromotion cmd) {
        Promotion promotion = load(cmd.getPromotionId());
        promotion.complete(cmd.getActingUser(), cmd.getNotes());
        repository.save(promotion);
        writeToOutbox(promotion);
        notificationPort.notifyTerminalState(promotion);
        log.info("Promotion {} completed", cmd.getPromotionId());
    }

    @Transactional
    public void handle(RollbackPromotion cmd) {
        Promotion promotion = load(cmd.getPromotionId());
        promotion.rollback(cmd.getActingUser(), cmd.getReason());
        repository.save(promotion);
        writeToOutbox(promotion);
        notificationPort.notifyTerminalState(promotion);
        log.info("Promotion {} rolled back: {}", cmd.getPromotionId(), cmd.getReason());
    }

    @Transactional
    public void handle(CancelPromotion cmd) {
        Promotion promotion = load(cmd.getPromotionId());
        promotion.cancel(cmd.getActingUser(), cmd.getReason());
        repository.save(promotion);
        writeToOutbox(promotion);
        notificationPort.notifyTerminalState(promotion);
        log.info("Promotion {} cancelled: {}", cmd.getPromotionId(), cmd.getReason());
    }

    // -------------------------------------------------------------------------

    private void writeToOutbox(Promotion promotion) {
        promotion.domainEvents().forEach(outboxWriter::write);
        promotion.clearDomainEvents();
    }

    private Promotion load(String promotionId) {
        return repository.findById(UUID.fromString(promotionId))
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));
    }
}
