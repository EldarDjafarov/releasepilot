package com.releasepilot.application.query.dto;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class PromotionDetailDto {
    UUID id;
    String applicationId;
    String version;
    Environment targetEnvironment;
    PromotionStatus status;
    String requestedBy;
    Instant requestedAt;
    String approvedBy;
    Instant approvedAt;
    String completionNotes;
    String rollbackReason;
    String cancellationReason;
    String releaseNotes;
    List<StateHistoryEntryDto> history;
}
