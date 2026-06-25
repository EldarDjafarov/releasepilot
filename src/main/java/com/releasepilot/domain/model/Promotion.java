package com.releasepilot.domain.model;

import com.releasepilot.domain.event.*;
import com.releasepilot.domain.exception.*;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Promotion aggregate root.
 *
 * Pure domain logic — no Spring, no JPA, no I/O.
 * Collects domain events internally; the application layer publishes them
 * after a successful persist (via @TransactionalEventListener AFTER_COMMIT).
 */
@Getter
public class Promotion {

    private final UUID id;
    private final String applicationId;
    private final String version;
    private final Environment targetEnvironment;
    private final String requestedBy;
    private final Instant requestedAt;

    private PromotionStatus status;
    private String approvedBy;
    private Instant approvedAt;
    private String completionNotes;
    private String rollbackReason;
    private String cancellationReason;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    public static Promotion request(
            String applicationId,
            String version,
            Environment targetEnvironment,
            String requestedBy,
            List<Environment> completedEnvironments) {

        validatePipelineOrder(applicationId, version, targetEnvironment, completedEnvironments);

        Promotion promotion = new Promotion(
                UUID.randomUUID(), applicationId, version, targetEnvironment, requestedBy
        );

        promotion.domainEvents.add(new PromotionRequestedEvent(
                promotion.id, requestedBy, applicationId, version, targetEnvironment
        ));

        return promotion;
    }

    public static Promotion reconstitute(
            UUID id, String applicationId, String version,
            Environment targetEnvironment, String requestedBy, Instant requestedAt,
            PromotionStatus status, String approvedBy, Instant approvedAt,
            String completionNotes, String rollbackReason, String cancellationReason) {

        Promotion p = new Promotion(id, applicationId, version, targetEnvironment, requestedBy, requestedAt);
        p.status = status;
        p.approvedBy = approvedBy;
        p.approvedAt = approvedAt;
        p.completionNotes = completionNotes;
        p.rollbackReason = rollbackReason;
        p.cancellationReason = cancellationReason;
        return p;
    }

    private Promotion(UUID id, String applicationId, String version,
                      Environment targetEnvironment, String requestedBy) {
        this(id, applicationId, version, targetEnvironment, requestedBy, Instant.now());
    }

    private Promotion(UUID id, String applicationId, String version,
                      Environment targetEnvironment, String requestedBy, Instant requestedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.version = version;
        this.targetEnvironment = targetEnvironment;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = PromotionStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void approve(String approverId, boolean isApprover) {
        guardNotTerminal();
        guardStatus(PromotionStatus.PENDING, "approve");

        if (!isApprover) throw new UnauthorizedApprovalException(approverId);

        this.status = PromotionStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();

        domainEvents.add(new PromotionApprovedEvent(id, approverId));
    }

    public void startDeployment(String actingUser) {
        guardNotTerminal();
        guardStatus(PromotionStatus.APPROVED, "start deployment");

        this.status = PromotionStatus.IN_PROGRESS;
        domainEvents.add(new DeploymentStartedEvent(id, actingUser));
    }

    public void complete(String actingUser, String notes) {
        guardNotTerminal();
        guardStatus(PromotionStatus.IN_PROGRESS, "complete");

        this.status = PromotionStatus.COMPLETED;
        this.completionNotes = notes;
        domainEvents.add(new PromotionCompletedEvent(id, actingUser));
    }

    public void rollback(String actingUser, String reason) {
        guardNotTerminal();
        guardStatus(PromotionStatus.IN_PROGRESS, "rollback");

        this.status = PromotionStatus.ROLLED_BACK;
        this.rollbackReason = reason;
        domainEvents.add(new PromotionRolledBackEvent(id, actingUser, reason));
    }

    public void cancel(String actingUser, String reason) {
        guardNotTerminal();
        if (status == PromotionStatus.IN_PROGRESS) {
            throw new InvalidPromotionStateException(
                    "Cannot cancel a promotion that is IN_PROGRESS. Use rollback instead.");
        }

        this.status = PromotionStatus.CANCELLED;
        this.cancellationReason = reason;
        domainEvents.add(new PromotionCancelledEvent(id, actingUser, reason));
    }

    // -------------------------------------------------------------------------
    // Guards
    // -------------------------------------------------------------------------

    private void guardNotTerminal() {
        if (status.isTerminal()) {
            throw new InvalidPromotionStateException(
                    "Promotion %s is in terminal state %s and cannot be modified".formatted(id, status));
        }
    }

    private void guardStatus(PromotionStatus required, String operation) {
        if (status != required) {
            throw new InvalidPromotionStateException(
                    "Cannot %s: promotion is in state %s, expected %s".formatted(operation, status, required));
        }
    }

    private static void validatePipelineOrder(String applicationId, String version,
                                               Environment target, List<Environment> completedEnvironments) {
        target.prerequisite().ifPresent(prereq -> {
            if (!completedEnvironments.contains(prereq)) {
                throw new EnvironmentSkippedException(
                        "Cannot promote '%s' v%s to %s: version has not completed %s yet"
                                .formatted(applicationId, version, target, prereq));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Event collection
    // -------------------------------------------------------------------------

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
