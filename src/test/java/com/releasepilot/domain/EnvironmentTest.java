package com.releasepilot.domain;

import com.releasepilot.domain.exception.EnvironmentSkippedException;
import com.releasepilot.domain.model.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Environment pipeline ordering")
class EnvironmentTest {

    @Test
    @DisplayName("DEV has no prerequisite")
    void devHasNoPrerequisite() {
        assertThat(Environment.DEV.prerequisite()).isEmpty();
    }

    @Test
    @DisplayName("STAGING prerequisite is DEV")
    void stagingPrerequisiteIsDev() {
        assertThat(Environment.STAGING.prerequisite()).contains(Environment.DEV);
    }

    @Test
    @DisplayName("PRODUCTION prerequisite is STAGING")
    void productionPrerequisiteIsStaging() {
        assertThat(Environment.PRODUCTION.prerequisite()).contains(Environment.STAGING);
    }

    @Test
    @DisplayName("DEV is the first environment")
    void devIsFirst() {
        assertThat(Environment.DEV.isFirst()).isTrue();
        assertThat(Environment.STAGING.isFirst()).isFalse();
        assertThat(Environment.PRODUCTION.isFirst()).isFalse();
    }

    @Test
    @DisplayName("PRODUCTION has no next environment")
    void productionHasNoNext() {
        assertThatThrownBy(Environment.PRODUCTION::next)
                .isInstanceOf(EnvironmentSkippedException.class);
    }

    @Test
    @DisplayName("DEV next is STAGING, STAGING next is PRODUCTION")
    void pipelineOrder() {
        assertThat(Environment.DEV.next()).isEqualTo(Environment.STAGING);
        assertThat(Environment.STAGING.next()).isEqualTo(Environment.PRODUCTION);
    }
}
