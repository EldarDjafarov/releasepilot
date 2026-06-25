package com.releasepilot.agent;

import com.releasepilot.domain.port.IssueTrackerPort;
import com.releasepilot.domain.port.WorkItem;
import com.releasepilot.infrastructure.agent.ReleaseNotesAgent;
import com.releasepilot.infrastructure.agent.llm.MockLlmClient;
import com.releasepilot.infrastructure.agent.tool.*;
import com.releasepilot.infrastructure.persistence.PromotionEntity;
import com.releasepilot.infrastructure.persistence.PromotionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Release Notes Agent")
class ReleaseNotesAgentTest {

    @Mock IssueTrackerPort issueTrackerPort;
    @Mock PromotionJpaRepository promotionRepository;

    private ReleaseNotesAgent agent;

    @BeforeEach
    void setUp() {
        List<AgentTool> tools = List.of(
                new GetWorkItemsTool(issueTrackerPort),
                new AskClarificationTool(),
                new FlagBreakingChangeTool(),
                new SubmitReleaseNotesTool(promotionRepository)
        );
        agent = new ReleaseNotesAgent(new MockLlmClient(), tools);
    }

    @Test
    @DisplayName("Agent completes full tool-calling loop in 4 iterations")
    void agentCompletesInFourIterations() {
        UUID promotionId = UUID.randomUUID();
        PromotionEntity promotion = new PromotionEntity();

        when(issueTrackerPort.fetchLinkedWorkItems(promotionId)).thenReturn(workItems());
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String notes = agent.generateReleaseNotes(promotionId);

        assertThat(notes).contains("## Release Notes");
        assertThat(notes).contains("OAuth2");
        assertThat(notes).contains("Breaking Changes");
        verify(issueTrackerPort).fetchLinkedWorkItems(promotionId);
        verify(promotionRepository).save(any());
    }

    @Test
    @DisplayName("SubmitReleaseNotes is the terminal tool")
    void submitReleaseNotesIsTerminal() {
        assertThat(new SubmitReleaseNotesTool(promotionRepository).isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Non-terminal tools are not terminal")
    void otherToolsAreNotTerminal() {
        assertThat(new GetWorkItemsTool(issueTrackerPort).isTerminal()).isFalse();
        assertThat(new FlagBreakingChangeTool().isTerminal()).isFalse();
        assertThat(new AskClarificationTool().isTerminal()).isFalse();
    }

    @Test
    @DisplayName("GetWorkItems returns formatted work item list")
    void getWorkItemsToolFormatsResult() {
        UUID promotionId = UUID.randomUUID();
        when(issueTrackerPort.fetchLinkedWorkItems(promotionId)).thenReturn(List.of(
                WorkItem.builder().id("PROJ-1").title("Test feature").type("FEATURE")
                        .status("DONE").description("A test feature").build()
        ));

        String result = new GetWorkItemsTool(issueTrackerPort)
                .execute(Map.of("promotionId", promotionId.toString()));

        assertThat(result).contains("PROJ-1").contains("Test feature");
    }

    @Test
    @DisplayName("GetWorkItems returns empty message when no items")
    void getWorkItemsToolHandlesEmpty() {
        UUID promotionId = UUID.randomUUID();
        when(issueTrackerPort.fetchLinkedWorkItems(promotionId)).thenReturn(List.of());

        String result = new GetWorkItemsTool(issueTrackerPort)
                .execute(Map.of("promotionId", promotionId.toString()));

        assertThat(result).contains("No work items");
    }

    @Test
    @DisplayName("FlagBreakingChange returns confirmation")
    void flagBreakingChangeTool() {
        String result = new FlagBreakingChangeTool().execute(
                Map.of("workItemId", "PROJ-103", "reason", "Removed deprecated API"));

        assertThat(result).contains("PROJ-103").contains("Removed deprecated API");
    }

    @Test
    @DisplayName("AskClarification returns acknowledgement")
    void askClarificationTool() {
        String result = new AskClarificationTool().execute(
                Map.of("workItemId", "PROJ-101", "question", "Is this breaking?"));

        assertThat(result).contains("PROJ-101");
    }

    @Test
    @DisplayName("SubmitReleaseNotes persists to promotion and returns draft")
    void submitReleaseNotesPersistsAndReturnsDraft() {
        UUID promotionId = UUID.randomUUID();
        String draft = "## Release Notes\n...";
        PromotionEntity promotion = new PromotionEntity();

        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = new SubmitReleaseNotesTool(promotionRepository)
                .execute(Map.of("promotionId", promotionId.toString(), "draft", draft));

        assertThat(result).isEqualTo(draft);
        verify(promotionRepository).save(argThat(p -> draft.equals(p.getReleaseNotes())));
    }

    @Test
    @DisplayName("SubmitReleaseNotes handles missing promotionId gracefully")
    void submitReleaseNotesHandlesMissingPromotionId() {
        String result = new SubmitReleaseNotesTool(promotionRepository)
                .execute(Map.of("draft", "## Notes"));

        assertThat(result).isEqualTo("## Notes");
        verifyNoInteractions(promotionRepository);
    }

    private List<WorkItem> workItems() {
        return List.of(
                WorkItem.builder().id("PROJ-101").title("OAuth2").type("FEATURE")
                        .status("DONE").description("Added OAuth2 login").build(),
                WorkItem.builder().id("PROJ-102").title("Memory leak").type("BUG")
                        .status("DONE").description("Fixed session eviction").build(),
                WorkItem.builder().id("PROJ-103").title("Spring Boot upgrade").type("CHORE")
                        .status("DONE").description("Upgraded to 3.5").build()
        );
    }
}
