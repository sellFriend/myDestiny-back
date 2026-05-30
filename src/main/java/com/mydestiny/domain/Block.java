package com.mydestiny.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "blocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Block {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "blocker_user_id", nullable = false, length = 36)
    private String blockerUserId;

    @Column(name = "blocked_acquaintance_id", nullable = false, length = 36)
    private String blockedAcquaintanceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
