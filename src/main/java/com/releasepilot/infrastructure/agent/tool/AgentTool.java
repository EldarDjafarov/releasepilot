package com.releasepilot.infrastructure.agent.tool;

import com.releasepilot.infrastructure.agent.model.ToolDefinition;

import java.util.Map;

/**
 * Interface for all agent tools.
 *
 * terminal() marks the tool that ends the loop — the agent stops
 * after calling it, regardless of the return value.
 * Only SubmitReleaseNotes is terminal.
 */
public interface AgentTool {

    String name();

    ToolDefinition definition();

    String execute(Map<String, String> arguments);

    default boolean isTerminal() {
        return false;
    }
}
