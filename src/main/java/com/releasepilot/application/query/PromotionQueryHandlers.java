package com.releasepilot.application.query;

import com.releasepilot.application.query.dto.*;
import com.releasepilot.domain.exception.PromotionNotFoundException;
import com.releasepilot.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionQueryHandlers {

    private final PromotionJpaRepository promotionRepo;
    private final PromotionEventJpaRepository eventRepo;

    @Transactional(readOnly = true)
    public PromotionDetailDto getPromotionDetail(UUID promotionId) {
        PromotionEntity entity = promotionRepo.findById(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId.toString()));

        List<PromotionEventEntity> events = eventRepo.findByPromotionIdOrderByOccurredAtAsc(promotionId);

        List<StateHistoryEntryDto> history = events.stream()
                .map(e -> StateHistoryEntryDto.builder()
                        .eventId(e.getId())
                        .eventType(e.getEventType())
                        .actingUser(e.getActingUser())
                        .occurredAt(e.getOccurredAt())
                        .reason(e.getReason())
                        .build())
                .toList();

        return PromotionDetailDto.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .version(entity.getVersion())
                .targetEnvironment(entity.getTargetEnvironment())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .requestedAt(entity.getRequestedAt())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .completionNotes(entity.getCompletionNotes())
                .rollbackReason(entity.getRollbackReason())
                .cancellationReason(entity.getCancellationReason())
                .releaseNotes(entity.getReleaseNotes())
                .history(history)
                .build();
    }

    @Transactional(readOnly = true)
    public ApplicationStatusDto getApplicationStatus(String applicationId) {
        List<PromotionEntity> latest = promotionRepo.findLatestPerEnvironmentForApplication(applicationId);

        List<EnvironmentStatusDto> envStatuses = latest.stream()
                .map(e -> EnvironmentStatusDto.builder()
                        .environment(e.getTargetEnvironment())
                        .latestVersion(e.getVersion())
                        .latestStatus(e.getStatus())
                        .lastUpdated(e.getUpdatedAt())
                        .build())
                .toList();

        return ApplicationStatusDto.builder()
                .applicationId(applicationId)
                .environments(envStatuses)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResultDto<PromotionSummaryDto> getApplicationPromotions(
            String applicationId, int page, int size) {

        Page<PromotionEntity> pageResult = promotionRepo.findByApplicationIdOrderByRequestedAtDesc(
                applicationId, PageRequest.of(page, size));

        List<PromotionSummaryDto> items = pageResult.getContent().stream()
                .map(e -> PromotionSummaryDto.builder()
                        .id(e.getId())
                        .version(e.getVersion())
                        .targetEnvironment(e.getTargetEnvironment())
                        .status(e.getStatus())
                        .requestedBy(e.getRequestedBy())
                        .requestedAt(e.getRequestedAt())
                        .build())
                .toList();

        return PagedResultDto.<PromotionSummaryDto>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(pageResult.getTotalElements())
                .build();
    }
}
