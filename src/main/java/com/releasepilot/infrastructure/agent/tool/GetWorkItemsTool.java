package com.releasepilot.infrastructure.agent.tool;

import com.releasepilot.domain.port.IssueTrackerPort;
import com.releasepilot.domain.port.WorkItem;
import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetWorkItemsTool implements AgentTool {

    private final IssueTrackerPort issueTrackerPort;

    @Override
    public String name() {
        return "GetWorkItems";
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .name(name())
                .description("Fetches work items (features, bugs, chores) linked to the promotion")
                .parameters(Map.of("promotionId", "UUID of the promotion"))
                .build();
    }

    @Override
    public String execute(Map<String, String> arguments) {
        UUID promotionId = UUID.fromString(arguments.get("promotionId"));
        List<WorkItem> items = issueTrackerPort.fetchLinkedWorkItems(promotionId);

        log.info("GetWorkItems: found {} items for promotion {}", items.size(), promotionId);

        if (items.isEmpty()) return "No work items linked to this promotion.";

        StringBuilder sb = new StringBuilder("Found " + items.size() + " work items:\n");
        for (WorkItem item : items) {
            sb.append("- [%s] %s (%s): %s\n"
                    .formatted(item.getId(), item.getTitle(), item.getType(), item.getDescription()));
        }
        return sb.toString();
    }
}
