package com.releasepilot.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RollbackPromotion {
    @NotBlank String promotionId;
    @NotBlank String actingUser;
    @NotBlank String reason;
}
