package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.EnvironmentSkippedException;

import java.util.List;
import java.util.Optional;

/**
 * Ordered pipeline of deployment environments.
 * The pipeline is the single source of truth for what "next" means.
 * No environment logic leaks into the aggregate or handlers.
 */
public enum Environment {
    DEV,
    STAGING,
    PRODUCTION;

    private static final List<Environment> PIPELINE = List.of(DEV, STAGING, PRODUCTION);

    public Environment next() {
        int idx = PIPELINE.indexOf(this);
        if (idx == PIPELINE.size() - 1) {
            throw new EnvironmentSkippedException("No environment after " + this + ": already at end of pipeline");
        }
        return PIPELINE.get(idx + 1);
    }

    /**
     * Returns the environment that must be completed before this one can be targeted.
     * DEV has no prerequisite.
     */
    public Optional<Environment> prerequisite() {
        int idx = PIPELINE.indexOf(this);
        if (idx == 0) return Optional.empty();
        return Optional.of(PIPELINE.get(idx - 1));
    }

    public boolean isFirst() {
        return this == PIPELINE.getFirst();
    }
}
