package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PromotionJpaRepository extends JpaRepository<PromotionEntity, UUID> {

    boolean existsByApplicationIdAndTargetEnvironmentAndStatusIn(
            String applicationId,
            Environment targetEnvironment,
            List<PromotionStatus> statuses
    );

    @Query("""
            SELECT p.targetEnvironment
            FROM PromotionEntity p
            WHERE p.applicationId = :applicationId
              AND p.version = :version
              AND p.status = 'COMPLETED'
            """)
    List<Environment> findCompletedEnvironments(
            @Param("applicationId") String applicationId,
            @Param("version") String version
    );

    Page<PromotionEntity> findByApplicationIdOrderByRequestedAtDesc(
            String applicationId, Pageable pageable);

    List<PromotionEntity> findByStatus(PromotionStatus status);

    @Query(value = """
            SELECT DISTINCT ON (target_environment) *
            FROM promotions
            WHERE application_id = :applicationId
            ORDER BY target_environment, requested_at DESC
            """, nativeQuery = true)
    List<PromotionEntity> findLatestPerEnvironmentForApplication(
            @Param("applicationId") String applicationId);
}
