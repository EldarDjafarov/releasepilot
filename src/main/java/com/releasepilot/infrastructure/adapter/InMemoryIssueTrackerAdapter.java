package com.releasepilot.infrastructure.adapter;

import com.releasepilot.domain.port.IssueTrackerPort;
import com.releasepilot.domain.port.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class InMemoryIssueTrackerAdapter implements IssueTrackerPort {

    @Override
    public List<WorkItem> fetchLinkedWorkItems(UUID promotionId) {
        log.debug("[STUB] Fetching work items for promotion {}", promotionId);
        return List.of(
                WorkItem.builder()
                        .id("PROJ-101").title("Add OAuth2 login").type("FEATURE")
                        .status("DONE").description("Implemented OAuth2 with Google and GitHub providers")
                        .build(),
                WorkItem.builder()
                        .id("PROJ-102").title("Fix memory leak in session store").type("BUG")
                        .status("DONE").description("Sessions were not being evicted after logout")
                        .build(),
                WorkItem.builder()
                        .id("PROJ-103").title("Upgrade Spring Boot to 3.5").type("CHORE")
                        .status("DONE").description("Dependency upgrade, breaking change: removed deprecated security config")
                        .build()
        );
    }
}
