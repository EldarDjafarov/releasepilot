package com.releasepilot.domain.port;

import com.releasepilot.domain.model.Promotion;

/**
 * Port for sending notifications on terminal promotion states.
 *
 * Terminal states: COMPLETED, ROLLED_BACK, CANCELLED.
 * Implemented as a stub — in production this would call Slack, PagerDuty, email, etc.
 */
public interface NotificationPort {

    void notifyTerminalState(Promotion promotion);
}
