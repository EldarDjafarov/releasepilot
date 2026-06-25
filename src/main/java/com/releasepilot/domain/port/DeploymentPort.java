package com.releasepilot.domain.port;

import com.releasepilot.domain.model.Environment;

import java.util.UUID;

/**
 * Port for triggering deployments in external systems (e.g. ArgoCD, Spinnaker, Jenkins).
 *
 * Placed in the domain layer because it expresses a capability the domain needs.
 * The implementation lives in infrastructure (adapter package) and is injected
 * by Spring — the domain never imports infrastructure.
 *
 * Current implementation: in-memory stub (see InMemoryDeploymentAdapter).
 */
public interface DeploymentPort {

    /**
     * Triggers a deployment for the given promotion.
     * This is a fire-and-forget call — the actual deployment result
     * comes back via a separate webhook/event in a real system.
     *
     * @return a deployment tracking ID from the external system
     */
    String triggerDeployment(UUID promotionId, String applicationId, String version, Environment environment);
}
