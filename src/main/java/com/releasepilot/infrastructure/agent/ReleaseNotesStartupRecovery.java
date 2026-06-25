package com.releasepilot.infrastructure.agent;

import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.infrastructure.persistence.PromotionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseNotesStartupRecovery {

    private final PromotionJpaRepository promotionRepository;
    private final ReleaseNotesAgent agent;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingReleaseNotes() {
        List<?> pending = promotionRepository.findByStatus(PromotionStatus.APPROVED)
                .stream()
                .filter(p -> p.getReleaseNotes() == null)
                .toList();

        if (pending.isEmpty()) {
            log.debug("Startup recovery: no pending release notes found");
            return;
        }

        log.info("Startup recovery: found {} approved promotions without release notes", pending.size());

        promotionRepository.findByStatus(PromotionStatus.APPROVED)
                .stream()
                .filter(p -> p.getReleaseNotes() == null)
                .forEach(p -> {
                    log.info("Startup recovery: triggering agent for promotion {}", p.getId());
                    agent.generateReleaseNotes(p.getId());
                });
    }
}
