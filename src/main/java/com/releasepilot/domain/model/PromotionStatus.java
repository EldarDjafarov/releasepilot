package com.releasepilot.domain.model;

/**
 * Finite states of the Promotion aggregate.
 *
 * State machine:
 *
 *  PENDING ──approve──► APPROVED ──start──► IN_PROGRESS ──complete──► COMPLETED
 *     │                    │                     │
 *     └──cancel──► CANCELLED                     └──rollback──► ROLLED_BACK
 */
public enum PromotionStatus {
    PENDING,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    ROLLED_BACK,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == ROLLED_BACK || this == CANCELLED;
    }
}
