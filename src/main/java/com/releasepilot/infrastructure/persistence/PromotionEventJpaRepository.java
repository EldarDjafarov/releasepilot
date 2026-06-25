package com.releasepilot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionEventJpaRepository extends JpaRepository<PromotionEventEntity, UUID> {

    List<PromotionEventEntity> findByPromotionIdOrderByOccurredAtAsc(UUID promotionId);
}
