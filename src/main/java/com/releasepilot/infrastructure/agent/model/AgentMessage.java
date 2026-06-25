package com.releasepilot.infrastructure.agent.model;

import lombok.Builder;
import lombok.Value;

/**
 * A message in the agent conversation history.
 * Follows OpenAI message format: system / user / assistant / tool.
 */
@Value
@Builder
public class AgentMessage {
    String role;
    String content;
    String toolCallId; // only for role=tool

    public static AgentMessage system(String content) {
        return AgentMessage.builder().role("system").content(content).build();
    }

    public static AgentMessage user(String content) {
        return AgentMessage.builder().role("user").content(content).build();
    }

    public static AgentMessage assistant(String content) {
        return AgentMessage.builder().role("assistant").content(content).build();
    }

    public static AgentMessage toolResult(String toolCallId, String content) {
        return AgentMessage.builder().role("tool").toolCallId(toolCallId).content(content).build();
    }
}
