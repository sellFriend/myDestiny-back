package com.mydestiny.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// 이메일 OTP 방식 미사용 — 엔티티 비활성화 (DB 테이블 없음)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FormEmailVerification {

    private String id;
    private String email;
    private String otpHash;
    private String submissionToken;
    private int attemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void expire() {
        this.expiresAt = LocalDateTime.now().minusSeconds(1);
    }

    public String markVerified() {
        this.verifiedAt = LocalDateTime.now();
        this.submissionToken = UUID.randomUUID().toString().replace("-", "");
        return this.submissionToken;
    }
}
