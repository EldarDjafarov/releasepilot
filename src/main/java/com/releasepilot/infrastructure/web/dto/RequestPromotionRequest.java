package com.releasepilot.infrastructure.web.dto;

import com.releasepilot.domain.model.Environment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestPromotionRequest {
    @NotBlank private String applicationId;
    @NotBlank private String version;
    @NotNull  private Environment targetEnvironment;
    @NotBlank private String requestedBy;
}
