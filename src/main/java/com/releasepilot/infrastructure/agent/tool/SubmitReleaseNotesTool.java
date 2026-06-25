package com.releasepilot.infrastructure.agent.tool;

import com.releasepilot.infrastructure.agent.model.ToolDefinition;
import com.releasepilot.infrastructure.persistence.PromotionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitReleaseNotesTool implements AgentTool {

    private final PromotionJpaRepository promotionRepository;

    @Override
    public String name() {
        return "SubmitReleaseNotes";
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .name(name())
                .description("Submits the final release notes draft. Call this when done.")
                .parameters(Map.of(
                        "promotionId", "UUID of the promotion",
                        "draft", "The complete release notes markdown text"
                ))
                .build();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String execute(Map<String, String> arguments) {
        String draft = arguments.get("draft");
        String promotionIdStr = arguments.get("promotionId");

        if (promotionIdStr != null) {
            try {
                UUID promotionId = UUID.fromString(promotionIdStr);
                promotionRepository.findById(promotionId).ifPresent(p -> {
                    p.setReleaseNotes(draft);
                    promotionRepository.save(p);
                    log.info("Release notes persisted for promotion {}", promotionId);
                });
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid promotionId in SubmitReleaseNotes: {}", promotionIdStr);
            }
        }

        return draft;
    }
}
