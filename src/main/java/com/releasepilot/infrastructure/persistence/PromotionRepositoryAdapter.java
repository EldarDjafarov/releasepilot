package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.port.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PromotionRepositoryAdapter implements PromotionRepository {

    private final PromotionJpaRepository jpaRepository;

    private static final List<PromotionStatus> ACTIVE_STATUSES = List.of(
            PromotionStatus.PENDING, PromotionStatus.APPROVED, PromotionStatus.IN_PROGRESS
    );

    @Override
    public void save(Promotion promotion) {
        jpaRepository.save(toEntity(promotion));
    }

    @Override
    public Optional<Promotion> findById(UUID promotionId) {
        return jpaRepository.findById(promotionId).map(this::toDomain);
    }

    @Override
    public boolean existsActivePromotionFor(String applicationId, Environment targetEnvironment) {
        return jpaRepository.existsByApplicationIdAndTargetEnvironmentAndStatusIn(
                applicationId, targetEnvironment, ACTIVE_STATUSES);
    }

    @Override
    public List<Environment> findCompletedEnvironmentsFor(String applicationId, String version) {
        return jpaRepository.findCompletedEnvironments(applicationId, version);
    }

    @Override
    public List<Promotion> findByApplicationId(String applicationId, int page, int size) {
        return jpaRepository.findByApplicationIdOrderByRequestedAtDesc(
                applicationId, PageRequest.of(page, size)
        ).getContent().stream().map(this::toDomain).toList();
    }

    // -------------------------------------------------------------------------

    private PromotionEntity toEntity(Promotion p) {
        return PromotionEntity.builder()
                .id(p.getId())
                .applicationId(p.getApplicationId())
                .version(p.getVersion())
                .targetEnvironment(p.getTargetEnvironment())
                .status(p.getStatus())
                .requestedBy(p.getRequestedBy())
                .requestedAt(p.getRequestedAt())
                .approvedBy(p.getApprovedBy())
                .approvedAt(p.getApprovedAt())
                .completionNotes(p.getCompletionNotes())
                .rollbackReason(p.getRollbackReason())
                .cancellationReason(p.getCancellationReason())
                .build();
    }

    private Promotion toDomain(PromotionEntity e) {
        return Promotion.reconstitute(
                e.getId(), e.getApplicationId(), e.getVersion(),
                e.getTargetEnvironment(), e.getRequestedBy(), e.getRequestedAt(),
                e.getStatus(), e.getApprovedBy(), e.getApprovedAt(),
                e.getCompletionNotes(), e.getRollbackReason(), e.getCancellationReason()
        );
    }
}
