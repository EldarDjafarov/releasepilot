package com.releasepilot.infrastructure.agent.tool;

import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AskClarificationTool implements AgentTool {

    @Override
    public String name() {
        return "AskClarification";
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .name(name())
                .description("Asks a clarifying question about a specific work item")
                .parameters(Map.of(
                        "workItemId", "ID of the work item",
                        "question", "The clarifying question to ask"
                ))
                .build();
    }

    @Override
    public String execute(Map<String, String> arguments) {
        String workItemId = arguments.get("workItemId");
        String question = arguments.get("question");

        // In production: post a comment to Jira / send Slack message
        log.info("AskClarification: [{}] {}", workItemId, question);

        return "Clarification noted for %s: '%s' — assuming standard implementation."
                .formatted(workItemId, question);
    }
}
