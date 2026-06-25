package com.releasepilot.infrastructure.agent.model;

import lombok.Builder;
import lombok.Value;

/**
 * Response from the LLM.
 * Either contains a tool call (agent wants to use a tool)
 * or a final text response (agent is done).
 */
@Value
@Builder
public class LlmResponse {
    boolean hasToolCall;
    ToolCall toolCall;      // present when hasToolCall = true
    String textContent;     // present when hasToolCall = false (final answer)

    public static LlmResponse withToolCall(ToolCall toolCall) {
        return LlmResponse.builder()
                .hasToolCall(true)
                .toolCall(toolCall)
                .build();
    }

    public static LlmResponse withText(String text) {
        return LlmResponse.builder()
                .hasToolCall(false)
                .textContent(text)
                .build();
    }
}
