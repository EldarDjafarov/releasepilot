package com.releasepilot.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApproveRequest {
    @NotBlank private String approverId;
}
