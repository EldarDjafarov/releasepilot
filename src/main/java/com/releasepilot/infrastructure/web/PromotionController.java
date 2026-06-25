package com.releasepilot.infrastructure.web;

import com.releasepilot.application.command.PromotionCommandHandlers;
import com.releasepilot.application.command.dto.*;
import com.releasepilot.application.query.PromotionQueryHandlers;
import com.releasepilot.application.query.dto.*;
import com.releasepilot.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionCommandHandlers commandHandlers;
    private final PromotionQueryHandlers queryHandlers;

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @PostMapping("/promotions")
    public ResponseEntity<Void> requestPromotion(@RequestBody @Valid RequestPromotionRequest req) {
        UUID id = commandHandlers.handle(RequestPromotion.builder()
                .applicationId(req.getApplicationId())
                .version(req.getVersion())
                .targetEnvironment(req.getTargetEnvironment())
                .requestedBy(req.getRequestedBy())
                .build());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/promotions/{id}/approve")
    public ResponseEntity<Void> approvePromotion(@PathVariable String id,
                                                  @RequestBody @Valid ApproveRequest req) {
        commandHandlers.handle(ApprovePromotion.builder()
                .promotionId(id)
                .approverId(req.getApproverId())
                .build());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promotions/{id}/start-deployment")
    public ResponseEntity<Void> startDeployment(@PathVariable String id,
                                                 @RequestBody @Valid ActingUserRequest req) {
        commandHandlers.handle(StartDeployment.builder()
                .promotionId(id)
                .actingUser(req.getActingUser())
                .build());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promotions/{id}/complete")
    public ResponseEntity<Void> completePromotion(@PathVariable String id,
                                                   @RequestBody @Valid CompleteRequest req) {
        commandHandlers.handle(CompletePromotion.builder()
                .promotionId(id)
                .actingUser(req.getActingUser())
                .notes(req.getNotes())
                .build());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promotions/{id}/rollback")
    public ResponseEntity<Void> rollbackPromotion(@PathVariable String id,
                                                   @RequestBody @Valid ReasonRequest req) {
        commandHandlers.handle(RollbackPromotion.builder()
                .promotionId(id)
                .actingUser(req.getActingUser())
                .reason(req.getReason())
                .build());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promotions/{id}/cancel")
    public ResponseEntity<Void> cancelPromotion(@PathVariable String id,
                                                 @RequestBody @Valid ReasonRequest req) {
        commandHandlers.handle(CancelPromotion.builder()
                .promotionId(id)
                .actingUser(req.getActingUser())
                .reason(req.getReason())
                .build());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @GetMapping("/promotions/{id}")
    public ResponseEntity<PromotionDetailDto> getPromotion(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandlers.getPromotionDetail(id));
    }

    @GetMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApplicationStatusDto> getApplicationStatus(@PathVariable String applicationId) {
        return ResponseEntity.ok(queryHandlers.getApplicationStatus(applicationId));
    }

    @GetMapping("/applications/{applicationId}/promotions")
    public ResponseEntity<PagedResultDto<PromotionSummaryDto>> getApplicationPromotions(
            @PathVariable String applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryHandlers.getApplicationPromotions(applicationId, page, size));
    }
}
