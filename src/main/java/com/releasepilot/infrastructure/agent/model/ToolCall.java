package com.releasepilot.infrastructure.agent.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * A tool call decision made by the LLM.
 */
@Value
@Builder
public class ToolCall {
    String id;
    String name;
    Map<String, String> arguments;
}
