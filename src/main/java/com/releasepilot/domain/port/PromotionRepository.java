package com.releasepilot.domain.port;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port — defined in the domain, implemented in infrastructure.
 *
 * The domain layer owns this interface. It expresses what the aggregate needs
 * from persistence in domain terms, not in JPA/SQL terms.
 * The infrastructure layer provides the implementation.
 */
public interface PromotionRepository {

    void save(Promotion promotion);

    Optional<Promotion> findById(UUID promotionId);

    /**
     * Used to enforce the "one active promotion per application + environment" invariant.
     */
    boolean existsActivePromotionFor(String applicationId, Environment targetEnvironment);

    /**
     * Returns all environments where this application version has a COMPLETED promotion.
     * Used to validate pipeline ordering on new promotion requests.
     */
    List<Environment> findCompletedEnvironmentsFor(String applicationId, String version);

    List<Promotion> findByApplicationId(String applicationId, int page, int size);
}
