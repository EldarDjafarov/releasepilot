package com.releasepilot.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
