package com.releasepilot.infrastructure.adapter;

import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryNotificationAdapter implements NotificationPort {

    @Override
    public void notifyTerminalState(Promotion promotion) {
        log.info("[STUB] Notification sent: promotion {} reached terminal state {} for app {} v{}",
                promotion.getId(), promotion.getStatus(),
                promotion.getApplicationId(), promotion.getVersion());
    }
}
