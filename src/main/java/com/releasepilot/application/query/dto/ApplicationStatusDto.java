package com.releasepilot.application.query.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ApplicationStatusDto {
    String applicationId;
    List<EnvironmentStatusDto> environments;
}
