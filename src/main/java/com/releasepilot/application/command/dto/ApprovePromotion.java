package com.releasepilot.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApprovePromotion {
    @NotBlank String promotionId;
    @NotBlank String approverId;
}
