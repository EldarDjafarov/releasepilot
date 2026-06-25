package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_environment", nullable = false)
    private Environment targetEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completion_notes")
    private String completionNotes;

    @Column(name = "rollback_reason")
    private String rollbackReason;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
