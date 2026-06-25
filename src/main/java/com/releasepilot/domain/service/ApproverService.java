package com.releasepilot.domain.service;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Domain service that answers: is this user an approver?
 *
 * In a real system this would delegate to an IAM/RBAC system.
 * Here it's a hardcoded set — enough to demonstrate the pattern
 * without coupling the aggregate to Spring Security.
 *
 * The aggregate calls this indirectly: the command handler resolves
 * the boolean and passes it into aggregate.approve(userId, isApprover).
 * The aggregate stays ignorant of where the answer came from.
 */
@Service
public class ApproverService {

    // In production: load from DB / LDAP / IAM
    private static final Set<String> APPROVERS = Set.of(
            "alice", "bob", "release-bot"
    );

    public boolean isApprover(String userId) {
        return APPROVERS.contains(userId);
    }
}
