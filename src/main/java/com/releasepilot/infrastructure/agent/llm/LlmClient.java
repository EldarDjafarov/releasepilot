package com.releasepilot.infrastructure.agent.llm;

import com.releasepilot.infrastructure.agent.model.AgentMessage;
import com.releasepilot.infrastructure.agent.model.LlmResponse;
import com.releasepilot.infrastructure.agent.model.ToolDefinition;

import java.util.List;

/**
 * Port for the LLM backend.
 *
 * To use a real LLM:
 *   - Implement this interface with an HTTP client calling OpenAI / LiteLLM proxy
 *   - Point LiteLLM at localhost:4000: litellm --model gpt-4o
 *   - Replace MockLlmClient @Primary with the real implementation
 *
 * The agent loop is completely decoupled from this — it only sees this interface.
 */
public interface LlmClient {

    LlmResponse complete(List<AgentMessage> messages, List<ToolDefinition> tools);
}
