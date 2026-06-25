package com.releasepilot.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteRequest {
    @NotBlank private String actingUser;
    private String notes;
}
