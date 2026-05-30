package com.mydestiny.repository;

import com.mydestiny.domain.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, String> {

    // 만료되지 않은 가장 최근 미인증 OTP 조회
    Optional<PhoneVerification> findTopByUserIdAndPurposeAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String userId, String purpose, java.time.LocalDateTime now);

    long countByUserIdAndCreatedAtAfter(String userId, java.time.LocalDateTime after);
}
