package com.releasepilot.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActingUserRequest {
    @NotBlank private String actingUser;
}
