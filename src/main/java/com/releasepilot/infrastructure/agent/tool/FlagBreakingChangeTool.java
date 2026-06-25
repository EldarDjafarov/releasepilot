package com.releasepilot.infrastructure.agent.tool;

import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class FlagBreakingChangeTool implements AgentTool {

    @Override
    public String name() {
        return "FlagBreakingChange";
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .name(name())
                .description("Flags a work item as containing a breaking change")
                .parameters(Map.of(
                        "workItemId", "ID of the work item",
                        "reason", "Why this is a breaking change"
                ))
                .build();
    }

    @Override
    public String execute(Map<String, String> arguments) {
        String workItemId = arguments.get("workItemId");
        String reason = arguments.get("reason");

        log.info("FlagBreakingChange: [{}] {}", workItemId, reason);

        return "Breaking change flagged for %s: %s".formatted(workItemId, reason);
    }
}
