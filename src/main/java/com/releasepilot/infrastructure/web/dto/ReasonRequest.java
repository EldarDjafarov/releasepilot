package com.releasepilot.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReasonRequest {
    @NotBlank private String actingUser;
    @NotBlank private String reason;
}
