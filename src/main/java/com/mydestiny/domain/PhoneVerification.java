package com.mydestiny.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PhoneVerification {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // BCrypt(otp) — 평문 OTP는 절대 저장하지 않음
    @Column(name = "otp_hash", nullable = false, length = 128)
    private String otpHash;

    @Column(nullable = false, length = 20)
    private String purpose; // "APPROVAL"

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    // 재발송 시 이전 OTP를 즉시 만료 (삭제하지 않고 누적하여 일일 한도 카운팅에 사용)
    public void expire() {
        this.expiresAt = LocalDateTime.now().minusSeconds(1);
    }
}
