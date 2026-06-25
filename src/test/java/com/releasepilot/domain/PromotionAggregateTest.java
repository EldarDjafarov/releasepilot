package com.releasepilot.domain;

import com.releasepilot.domain.event.*;
import com.releasepilot.domain.exception.*;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure unit tests for the Promotion aggregate.
 *
 * No Spring, no database, no mocks — just the domain model.
 * These run in milliseconds and test every invariant.
 */
class PromotionAggregateTest {

    // -------------------------------------------------------------------------
    // Factory / RequestPromotion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Requesting a promotion")
    class RequestPromotion {

        @Test
        @DisplayName("DEV promotion requires no prerequisites")
        void devPromotionNeedsNoPrerequisites() {
            Promotion p = Promotion.request("app-1", "1.0", Environment.DEV, "alice", List.of());

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.PENDING);
            assertThat(p.getTargetEnvironment()).isEqualTo(Environment.DEV);
            assertThat(p.domainEvents()).hasSize(1);
            assertThat(p.domainEvents().getFirst()).isInstanceOf(PromotionRequestedEvent.class);
        }

        @Test
        @DisplayName("STAGING promotion requires DEV to be completed")
        void stagingRequiresDev() {
            Promotion p = Promotion.request("app-1", "1.0", Environment.STAGING,
                    "alice", List.of(Environment.DEV));

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.PENDING);
        }

        @Test
        @DisplayName("STAGING promotion fails if DEV not completed — EnvironmentSkipped")
        void stagingFailsWithoutDev() {
            assertThatThrownBy(() ->
                    Promotion.request("app-1", "1.0", Environment.STAGING, "alice", List.of())
            )
                    .isInstanceOf(EnvironmentSkippedException.class)
                    .hasMessageContaining("has not completed DEV");
        }

        @Test
        @DisplayName("PRODUCTION promotion fails if only DEV is completed")
        void productionFailsWithOnlyDev() {
            assertThatThrownBy(() ->
                    Promotion.request("app-1", "1.0", Environment.PRODUCTION,
                            "alice", List.of(Environment.DEV))
            )
                    .isInstanceOf(EnvironmentSkippedException.class)
                    .hasMessageContaining("has not completed STAGING");
        }

        @Test
        @DisplayName("PRODUCTION promotion succeeds if both DEV and STAGING are completed")
        void productionSucceedsWithFullPipeline() {
            Promotion p = Promotion.request("app-1", "1.0", Environment.PRODUCTION,
                    "alice", List.of(Environment.DEV, Environment.STAGING));

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.PENDING);
        }
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Approval")
    class Approval {

        @Test
        @DisplayName("Approver can approve a PENDING promotion")
        void approverCanApprove() {
            Promotion p = pendingPromotion();
            p.approve("alice", true);

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.APPROVED);
            assertThat(p.getApprovedBy()).isEqualTo("alice");
            assertThat(domainEventTypes(p)).contains("PromotionApproved");
        }

        @Test
        @DisplayName("Non-approver cannot approve — UnauthorizedApproval")
        void nonApproverCannotApprove() {
            Promotion p = pendingPromotion();

            assertThatThrownBy(() -> p.approve("charlie", false))
                    .isInstanceOf(UnauthorizedApprovalException.class)
                    .hasMessageContaining("charlie");
        }

        @Test
        @DisplayName("Cannot approve an already-approved promotion")
        void cannotApproveApprovedPromotion() {
            Promotion p = approvedPromotion();

            assertThatThrownBy(() -> p.approve("alice", true))
                    .isInstanceOf(InvalidPromotionStateException.class);
        }
    }

    @Nested
    @DisplayName("Deployment lifecycle")
    class DeploymentLifecycle {

        @Test
        @DisplayName("Full happy path: PENDING → APPROVED → IN_PROGRESS → COMPLETED")
        void fullHappyPath() {
            Promotion p = pendingPromotion();

            p.approve("alice", true);
            assertThat(p.getStatus()).isEqualTo(PromotionStatus.APPROVED);

            p.startDeployment("deploy-bot");
            assertThat(p.getStatus()).isEqualTo(PromotionStatus.IN_PROGRESS);

            p.complete("deploy-bot", "All checks passed");
            assertThat(p.getStatus()).isEqualTo(PromotionStatus.COMPLETED);
            assertThat(p.getStatus().isTerminal()).isTrue();

            assertThat(domainEventTypes(p)).containsExactly(
                    "PromotionRequested", "PromotionApproved", "DeploymentStarted", "PromotionCompleted"
            );
        }

        @Test
        @DisplayName("Rollback is only valid from IN_PROGRESS")
        void rollbackFromInProgress() {
            Promotion p = inProgressPromotion();

            p.rollback("alice", "Health checks failed");

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.ROLLED_BACK);
            assertThat(p.getRollbackReason()).isEqualTo("Health checks failed");
        }

        @Test
        @DisplayName("Cannot rollback from PENDING — use cancel instead")
        void cannotRollbackFromPending() {
            Promotion p = pendingPromotion();

            assertThatThrownBy(() -> p.rollback("alice", "reason"))
                    .isInstanceOf(InvalidPromotionStateException.class);
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class Cancellation {

        @Test
        @DisplayName("Can cancel a PENDING promotion")
        void cancelPending() {
            Promotion p = pendingPromotion();
            p.cancel("alice", "Wrong version");

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Can cancel an APPROVED promotion")
        void cancelApproved() {
            Promotion p = approvedPromotion();
            p.cancel("alice", "Blocking issue found");

            assertThat(p.getStatus()).isEqualTo(PromotionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Cannot cancel IN_PROGRESS — use rollback")
        void cannotCancelInProgress() {
            Promotion p = inProgressPromotion();

            assertThatThrownBy(() -> p.cancel("alice", "reason"))
                    .isInstanceOf(InvalidPromotionStateException.class)
                    .hasMessageContaining("rollback");
        }
    }

    @Nested
    @DisplayName("Terminal state immutability")
    class TerminalImmutability {

        @Test
        @DisplayName("Cannot modify a COMPLETED promotion")
        void completedIsImmutable() {
            Promotion p = completedPromotion();

            assertThatThrownBy(() -> p.approve("alice", true))
                    .isInstanceOf(InvalidPromotionStateException.class)
                    .hasMessageContaining("terminal");

            assertThatThrownBy(() -> p.cancel("alice", "reason"))
                    .isInstanceOf(InvalidPromotionStateException.class);
        }

        @Test
        @DisplayName("Cannot modify a ROLLED_BACK promotion")
        void rolledBackIsImmutable() {
            Promotion p = inProgressPromotion();
            p.rollback("alice", "failed");

            assertThatThrownBy(() -> p.startDeployment("bot"))
                    .isInstanceOf(InvalidPromotionStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Promotion pendingPromotion() {
        return Promotion.request("app-1", "1.0", Environment.DEV, "alice", List.of());
    }

    private Promotion approvedPromotion() {
        Promotion p = pendingPromotion();
        p.approve("alice", true);
        p.clearDomainEvents();
        return p;
    }

    private Promotion inProgressPromotion() {
        Promotion p = approvedPromotion();
        p.startDeployment("bot");
        p.clearDomainEvents();
        return p;
    }

    private Promotion completedPromotion() {
        Promotion p = inProgressPromotion();
        p.complete("bot", "done");
        p.clearDomainEvents();
        return p;
    }

    private List<String> domainEventTypes(Promotion p) {
        return p.domainEvents().stream().map(DomainEvent::eventType).toList();
    }
}
