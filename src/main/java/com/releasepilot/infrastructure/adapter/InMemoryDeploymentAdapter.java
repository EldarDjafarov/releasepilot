package com.releasepilot.infrastructure.adapter;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.port.DeploymentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class InMemoryDeploymentAdapter implements DeploymentPort {

    @Override
    public String triggerDeployment(UUID promotionId, String applicationId,
                                    String version, Environment environment) {
        String deploymentId = "deploy-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[STUB] Deployment triggered: promotionId={} app={} version={} env={} → deploymentId={}",
                promotionId, applicationId, version, environment, deploymentId);
        return deploymentId;
    }
}
