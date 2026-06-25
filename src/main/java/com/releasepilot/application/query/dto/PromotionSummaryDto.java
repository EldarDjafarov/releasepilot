package com.releasepilot.application.query.dto;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PromotionSummaryDto {
    UUID id;
    String version;
    Environment targetEnvironment;
    PromotionStatus status;
    String requestedBy;
    Instant requestedAt;
}
