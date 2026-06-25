package com.releasepilot.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompletePromotion {
    @NotBlank String promotionId;
    @NotBlank String actingUser;
    String notes;
}
