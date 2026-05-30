package com.mydestiny.domain;

import com.mydestiny.domain.enums.MatchingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchLog {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private Matching matching;

    // REQUESTER | RECEIVER | CANDIDATE | SYSTEM
    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;

    // 시스템 이벤트면 null
    @Column(name = "actor_id", length = 36)
    private String actorId;

    // REQUESTED | ACCEPTED | REJECTED | CONSENTED | DECLINED | CANCELLED | EXPIRED
    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "previous_status", length = 40)
    private String previousStatus;

    @Column(name = "next_status", length = 40)
    private String nextStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }

    public static MatchLog of(Matching matching, String actorType, String actorId,
                              String action, MatchingStatus previous, MatchingStatus next) {
        return MatchLog.builder()
                .matching(matching)
                .actorType(actorType)
                .actorId(actorId)
                .action(action)
                .previousStatus(previous != null ? previous.name() : null)
                .nextStatus(next != null ? next.name() : null)
                .build();
    }
}
