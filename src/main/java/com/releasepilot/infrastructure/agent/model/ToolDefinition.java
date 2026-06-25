package com.releasepilot.infrastructure.agent.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Describes a tool available to the LLM.
 * Follows OpenAI function-calling schema.
 */
@Value
@Builder
public class ToolDefinition {
    String name;
    String description;
    Map<String, String> parameters; // parameter name → description
}
