package com.releasepilot.application.query.dto;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class EnvironmentStatusDto {
    Environment environment;
    String latestVersion;
    PromotionStatus latestStatus;
    Instant lastUpdated;
}
