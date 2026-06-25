package com.releasepilot.domain.exception;

public class UnauthorizedApprovalException extends DomainException {
    public UnauthorizedApprovalException(String userId) {
        super("User '%s' is not an approver and cannot approve promotions".formatted(userId));
    }
}
