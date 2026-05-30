package com.mydestiny.domain;

import com.mydestiny.domain.enums.MatchingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matchings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Matching {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;          // A: 매칭 요청을 보낸 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;           // C: targetProfile 등록자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_profile_id", nullable = false)
    private DatingProfile requesterProfile;  // B: A가 등록한 친구 프로필

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private DatingProfile targetProfile;     // D: C가 등록한 친구 프로필

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private MatchingStatus status = MatchingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_id")
    private User cancelledBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "receiver_responded_at")
    private LocalDateTime receiverRespondedAt;

    // 수신자 응답 기한 (생성 시 +72h)
    @Column(name = "receiver_expires_at", nullable = false)
    private LocalDateTime receiverExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void acceptByReceiver() {
        if (status != MatchingStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 수락할 수 있습니다.");
        }
        this.status = MatchingStatus.ACCEPTED_BY_RECEIVER;
        this.receiverRespondedAt = LocalDateTime.now();
    }

    // 당사자 동의 없이 수신자 수락 즉시 성사
    public void matchDirectlyByReceiver() {
        if (status != MatchingStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 수락할 수 있습니다.");
        }
        this.status = MatchingStatus.MATCHED;
        this.receiverRespondedAt = LocalDateTime.now();
    }

    public void rejectByReceiver(String reason) {
        if (status != MatchingStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 거절할 수 있습니다.");
        }
        this.status = MatchingStatus.REJECTED_BY_RECEIVER;
        this.rejectReason = reason;
        this.receiverRespondedAt = LocalDateTime.now();
    }

    public void cancel(User by, String reason) {
        if (status != MatchingStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 취소할 수 있습니다.");
        }
        this.status = MatchingStatus.CANCELLED;
        this.cancelledBy = by;
        this.cancelReason = reason;
    }

    // acceptByReceiver() 직후 트랜잭션 내에서 호출
    public void transitionToConsentPending() {
        if (status != MatchingStatus.ACCEPTED_BY_RECEIVER) {
            throw new IllegalStateException("ACCEPTED_BY_RECEIVER 상태에서만 전이 가능합니다.");
        }
        this.status = MatchingStatus.CONSENT_PENDING;
    }

    public void markPartiallyApproved() {
        if (status != MatchingStatus.CONSENT_PENDING) {
            throw new IllegalStateException("CONSENT_PENDING 상태에서만 전이 가능합니다.");
        }
        this.status = MatchingStatus.CONSENT_PARTIALLY_APPROVED;
    }

    public void markMatched() {
        if (status != MatchingStatus.CONSENT_PENDING && status != MatchingStatus.CONSENT_PARTIALLY_APPROVED) {
            throw new IllegalStateException("동의 진행 상태에서만 MATCHED로 전이 가능합니다.");
        }
        this.status = MatchingStatus.MATCHED;
    }

    public void markConsentRejected() {
        this.status = MatchingStatus.CONSENT_REJECTED;
    }

    public void expire() {
        if (status != MatchingStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 만료됩니다.");
        }
        this.status = MatchingStatus.EXPIRED;
    }

    public void consentExpire() {
        this.status = MatchingStatus.CONSENT_EXPIRED;
    }

    public boolean isReceiverResponseOverdue() {
        return LocalDateTime.now().isAfter(receiverExpiresAt);
    }
}
