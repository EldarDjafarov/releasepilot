package com.releasepilot.domain.port;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkItem {
    String id;
    String title;
    String type;
    String status;
    String description;
}
