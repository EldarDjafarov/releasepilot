package com.releasepilot.application.command.dto;

import com.releasepilot.domain.model.Environment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequestPromotion {
    @NotBlank String applicationId;
    @NotBlank String version;
    @NotNull  Environment targetEnvironment;
    @NotBlank String requestedBy;
}
