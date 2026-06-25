package com.releasepilot.domain.port;

import java.util.List;
import java.util.UUID;

public interface IssueTrackerPort {

    List<WorkItem> fetchLinkedWorkItems(UUID promotionId);
}
