package com.mydestiny.domain;

import com.mydestiny.domain.enums.ConsentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "match_candidate_consents",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_consent_per_matching_profile",
        columnNames = {"matching_id", "profile_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchCandidateConsent {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private Matching matching;

    // B 또는 D의 DatingProfile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private DatingProfile profile;

    // 프로필 본인 (DatingProfile.subject) — 동의 권한 검증에 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    // 향후 SMS/이메일 링크 진입 지원용 토큰 (MVP: 인앱에서 ownerUser 확인으로 대체)
    @Column(name = "consent_token", nullable = false, unique = true, length = 128)
    private String consentToken;

    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    // 동의 기한 (생성 시 +48h)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }

    public void approve() {
        if (status != ConsentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 동의할 수 있습니다.");
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new IllegalStateException("동의 기한이 만료되었습니다.");
        }
        this.status = ConsentStatus.APPROVED;
        this.consentedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != ConsentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 거절할 수 있습니다.");
        }
        this.status = ConsentStatus.REJECTED;
        this.consentedAt = LocalDateTime.now();
    }

    public void expire() {
        if (status == ConsentStatus.PENDING) {
            this.status = ConsentStatus.EXPIRED;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isOwnedBy(String userId) {
        return ownerUser.getId().equals(userId);
    }
}
