package com.releasepilot.infrastructure.agent;

import com.releasepilot.infrastructure.agent.llm.LlmClient;
import com.releasepilot.infrastructure.agent.model.AgentMessage;
import com.releasepilot.infrastructure.agent.model.LlmResponse;
import com.releasepilot.infrastructure.agent.model.ToolCall;
import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import com.releasepilot.infrastructure.agent.tool.AgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseNotesAgent {

    private static final int MAX_ITERATIONS = 10;

    private final LlmClient llmClient;
    private final List<AgentTool> tools;

    public String generateReleaseNotes(UUID promotionId) {
        log.info("Release notes agent starting for promotion {}", promotionId);

        Map<String, AgentTool> toolRegistry = tools.stream()
                .collect(Collectors.toMap(AgentTool::name, Function.identity()));

        List<ToolDefinition> toolDefinitions = tools.stream()
                .map(AgentTool::definition)
                .toList();

        List<AgentMessage> messages = new ArrayList<>();
        messages.add(AgentMessage.system(systemPrompt()));
        messages.add(AgentMessage.system("promotionId=" + promotionId + "\n"));
        messages.add(AgentMessage.user(
                "Generate release notes for the promotion. Start by fetching the linked work items."
        ));

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.debug("Agent iteration {}/{}", i + 1, MAX_ITERATIONS);

            LlmResponse response = llmClient.complete(messages, toolDefinitions);

            if (!response.isHasToolCall()) {
                log.info("Agent finished with text response after {} iterations", i + 1);
                return response.getTextContent();
            }

            ToolCall toolCall = response.getToolCall();
            log.info("Agent calling tool: {} with args: {}", toolCall.getName(), toolCall.getArguments());

            messages.add(AgentMessage.assistant("Calling tool: " + toolCall.getName()));

            AgentTool tool = toolRegistry.get(toolCall.getName());
            if (tool == null) {
                log.error("Unknown tool requested: {}", toolCall.getName());
                messages.add(AgentMessage.toolResult(toolCall.getId(),
                        "Error: unknown tool " + toolCall.getName()));
                continue;
            }

            String toolResult = tool.execute(toolCall.getArguments());

            // Terminal tool ends the loop — no string parsing needed
            if (tool.isTerminal()) {
                log.info("Agent completed in {} iterations for promotion {}", i + 1, promotionId);
                return toolResult;
            }

            messages.add(AgentMessage.toolResult(toolCall.getId(), toolResult));
        }

        log.warn("Agent hit max iterations ({}) for promotion {}", MAX_ITERATIONS, promotionId);
        return "Release notes generation incomplete — max iterations reached.";
    }

    private String systemPrompt() {
        return """
                You are a release notes agent for a software deployment platform.
                Your goal is to generate clear, concise release notes for a promotion.
                
                Steps:
                1. Call GetWorkItems to fetch linked work items
                2. Review each item — call FlagBreakingChange for any breaking changes
                3. If a work item is unclear, call AskClarification (max once per item)
                4. Draft release notes grouped by: Features, Bug Fixes, Breaking Changes, Other
                5. Call SubmitReleaseNotes with promotionId and the final draft
                
                Be concise. Target audience is engineers.
                """;
    }
}
