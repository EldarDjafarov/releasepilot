package com.releasepilot.infrastructure.agent.llm;

import com.releasepilot.infrastructure.agent.model.AgentMessage;
import com.releasepilot.infrastructure.agent.model.LlmResponse;
import com.releasepilot.infrastructure.agent.model.ToolCall;
import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Primary
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(List<AgentMessage> messages, List<ToolDefinition> tools) {
        long step = messages.stream()
                .filter(m -> "assistant".equals(m.getRole()) || "tool".equals(m.getRole()))
                .count();

        log.debug("[MOCK LLM] Step {} — deciding next action", step);

        String promotionId = findContext(messages, "promotionId");

        return switch ((int) step) {
            case 0 -> LlmResponse.withToolCall(ToolCall.builder()
                    .id(UUID.randomUUID().toString())
                    .name("GetWorkItems")
                    .arguments(Map.of("promotionId", promotionId))
                    .build());

            case 2 -> LlmResponse.withToolCall(ToolCall.builder()
                    .id(UUID.randomUUID().toString())
                    .name("FlagBreakingChange")
                    .arguments(Map.of(
                            "workItemId", "PROJ-103",
                            "reason", "Removed deprecated Spring Security config — callers must migrate to SecurityFilterChain"
                    ))
                    .build());

            case 4 -> LlmResponse.withToolCall(ToolCall.builder()
                    .id(UUID.randomUUID().toString())
                    .name("AskClarification")
                    .arguments(Map.of(
                            "workItemId", "PROJ-101",
                            "question", "Does OAuth2 replace existing session login or add it as an additional option?"
                    ))
                    .build());

            case 6 -> LlmResponse.withToolCall(ToolCall.builder()
                    .id(UUID.randomUUID().toString())
                    .name("SubmitReleaseNotes")
                    .arguments(Map.of(
                            "promotionId", promotionId,
                            "draft", buildReleaseNotes()
                    ))
                    .build());

            default -> LlmResponse.withText("Release notes successfully generated.");
        };
    }

    private String findContext(List<AgentMessage> messages, String key) {
        return messages.stream()
                .filter(m -> "system".equals(m.getRole()) && m.getContent().contains(key + "="))
                .findFirst()
                .map(m -> {
                    String content = m.getContent();
                    int start = content.indexOf(key + "=") + key.length() + 1;
                    int end = content.indexOf("\n", start);
                    return end == -1 ? content.substring(start).trim() : content.substring(start, end).trim();
                })
                .orElse("unknown");
    }

    private String buildReleaseNotes() {
        return """
                ## Release Notes

                ### ✨ Features
                - **OAuth2 Login** (PROJ-101): Added OAuth2 authentication with Google and GitHub \
                as an additional login option alongside existing session-based auth.

                ### 🐛 Bug Fixes
                - **Session Store Memory Leak** (PROJ-102): Fixed sessions not being evicted \
                after logout, causing gradual memory increase.

                ### ⚠️ Breaking Changes
                - **Spring Boot Upgrade** (PROJ-103): Removed deprecated Spring Security \
                configuration. Migrate from WebSecurityConfigurerAdapter to SecurityFilterChain.

                ### 🔧 Other
                - Upgraded Spring Boot to 3.5 (PROJ-103)
                """;
    }
}
