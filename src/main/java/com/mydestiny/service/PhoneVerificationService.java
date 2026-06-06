package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Invitation;
import com.mydestiny.domain.PhoneVerification;
import com.mydestiny.domain.User;
import com.mydestiny.dto.invitation.OtpVerifyRequest;
import com.mydestiny.dto.invitation.OtpVerifyResponse;
import com.mydestiny.dto.invitation.PhoneVerificationSendRequest;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.InvitationRepository;
import com.mydestiny.repository.PhoneVerificationRepository;
import com.mydestiny.repository.UserRepository;
import com.mydestiny.service.sms.SmsService;
import com.mydestiny.util.PhoneEncryptionUtil;
import com.mydestiny.util.PhoneHashUtil;
import com.mydestiny.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationRepository verificationRepository;
    private final InvitationRepository invitationRepository;
    private final DatingProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PhoneHashUtil phoneHashUtil;
    private final PhoneEncryptionUtil phoneEncryptionUtil;
    private final PasswordEncoder passwordEncoder;
    private final InvitationService invitationService;
    private final SmsService smsService;

    @Transactional
    public void sendOtp(String userId, PhoneVerificationSendRequest req) {
        // 초대 유효성 확인
        Invitation invitation = invitationService.findValidInvitation(req.invitationToken());

        // 등록자(마담) 본인은 초대를 진행할 수 없음 — 진입 시점 차단
        assertNotRegistrant(invitation.getProfile(), userId, req.phone());

        // TODO: 테스트 완료 후 아래 한도 검사 재활성화
        // long todayCount = verificationRepository.countByUserIdAndCreatedAtAfter(
        //         userId, java.time.LocalDate.now().atStartOfDay());
        // if (todayCount >= 5) {
        //     throw new BusinessException("일일 OTP 발송 한도(5회)를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);
        // }

        // 기존 미인증 OTP 만료 처리 (삭제하지 않아야 일일 카운트에 누적됨)
        verificationRepository.findTopByUserIdAndPurposeAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                userId, "APPROVAL", java.time.LocalDateTime.now())
                .ifPresent(v -> {
                    v.expire();
                    verificationRepository.save(v);
                });

        String otp = TokenUtil.generateOtp();

        verificationRepository.save(PhoneVerification.builder()
                .user(userRepository.getReferenceById(userId))
                .otpHash(passwordEncoder.encode(otp))
                .purpose("APPROVAL")
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build());

        // SMS_PROVIDER=email 이면 email로, 아니면 phone 번호로 발송
        String destination = (req.email() != null && !req.email().isBlank())
                ? req.email()
                : req.phone();
        smsService.send(destination, "[My Destiny] 인증번호 [" + otp + "]를 입력해 주세요.");
    }

    @Transactional
    public OtpVerifyResponse verifyOtp(String userId, OtpVerifyRequest req) {
        Invitation invitation = invitationService.findValidInvitation(req.invitationToken());
        DatingProfile profile = invitation.getProfile();

        // 등록자(마담) 본인 차단 — sendOtp에서 1차 차단, 여기서도 방어
        assertNotRegistrant(profile, userId, req.phone());

        // 1. 번호가 A가 등록한 번호와 일치하는지 확인
        if (!phoneHashUtil.matches(req.phone(), profile.getSubjectPhoneHash())) {
            throw new BusinessException("등록된 전화번호와 일치하지 않습니다.", HttpStatus.FORBIDDEN);
        }

        // 2. 미인증 OTP 조회 (만료되지 않은 가장 최근 것)
        PhoneVerification verification = verificationRepository
                .findTopByUserIdAndPurposeAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, "APPROVAL", java.time.LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("OTP를 먼저 요청해 주세요.", HttpStatus.BAD_REQUEST));

        if (verification.isExpired()) {
            throw new BusinessException("OTP가 만료됐습니다. 다시 요청해 주세요.", HttpStatus.GONE);
        }
        if (verification.getAttemptCount() >= 3) {
            throw new BusinessException("OTP 시도 횟수를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);
        }

        verification.incrementAttempt();

        if (!passwordEncoder.matches(req.otp(), verification.getOtpHash())) {
            verificationRepository.save(verification);
            throw new BusinessException("OTP가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 3. OTP 인증 성공
        verification.markVerified();
        verificationRepository.save(verification);

        // 4. B의 전화번호를 User에 저장
        User subject = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        String normalized = phoneHashUtil.normalize(req.phone());
        subject.updatePhoneNumber(phoneHashUtil.hash(req.phone()), phoneEncryptionUtil.encrypt(normalized));
        userRepository.save(subject);

        // 5. subject 연결 (동일인 케이스는 진입 시점에 모두 차단됨 → 항상 정상 승인 대기)
        profile.linkSubject(subject, false);
        profileRepository.save(profile);

        // 초대 링크는 승인/거절 시점에 markUsed() — 여기서는 유지

        return new OtpVerifyResponse(profile.getId(), profile.getStatus().name());
    }

    // 등록자(마담) 본인은 초대를 진행할 수 없음 — 같은 카카오 계정이거나 등록자와 동일한 전화번호인 경우 차단
    private void assertNotRegistrant(DatingProfile profile, String userId, String phone) {
        User registrant = profile.getRegistrant();
        if (registrant.getId().equals(userId)) {
            throw new BusinessException("본인 계정으로는 초대를 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (registrant.getPhoneNumberHash() != null
                && phoneHashUtil.matches(phone, registrant.getPhoneNumberHash())) {
            throw new BusinessException("등록자 본인 번호로는 초대를 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }
}
