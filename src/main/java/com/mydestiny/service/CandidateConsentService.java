package com.mydestiny.service;

import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.Matching;
import com.mydestiny.domain.MatchLog;
import com.mydestiny.domain.enums.ConsentStatus;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.dto.matching.ConsentApproveResponse;
import com.mydestiny.dto.matching.ConsentResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.MatchCandidateConsentRepository;
import com.mydestiny.repository.MatchLogRepository;
import com.mydestiny.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateConsentService {

    private final MatchCandidateConsentRepository consentRepository;
    private final MatchingRepository matchingRepository;
    private final MatchLogRepository matchLogRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ConsentResponse> getPendingConsents(String userId) {
        return consentRepository
                .findByOwnerUserIdAndStatusOrderByCreatedAtDesc(userId, ConsentStatus.PENDING)
                .stream()
                .map(ConsentResponse::from)
                .toList();
    }

    public ConsentApproveResponse approveConsent(String consentId, String userId) {
        MatchCandidateConsent consent = findWithAccessCheck(consentId, userId);

        MatchingStatus prevMatchingStatus = consent.getMatching().getStatus();
        consent.approve(); // PENDING 및 만료 여부는 엔티티 내부에서 검증
        consentRepository.save(consent);

        Matching matching = consent.getMatching();
        long approvedCount = consentRepository.countByMatchingIdAndStatus(
                matching.getId(), ConsentStatus.APPROVED);

        if (approvedCount == 2) {
            // 양쪽 모두 동의 → MATCHED
            matching.markMatched();
            matchingRepository.save(matching);

            matchLogRepository.save(MatchLog.of(matching, "CANDIDATE", userId, "CONSENTED",
                    prevMatchingStatus, MatchingStatus.MATCHED));

            notifyAll(matching, NotificationType.MATCHED);
        } else {
            // 한 명만 동의 → CONSENT_PARTIALLY_APPROVED
            matching.markPartiallyApproved();
            matchingRepository.save(matching);

            matchLogRepository.save(MatchLog.of(matching, "CANDIDATE", userId, "CONSENTED",
                    prevMatchingStatus, MatchingStatus.CONSENT_PARTIALLY_APPROVED));

            // 상대 후보에게: 상대방이 동의했음
            String otherCandidateId = findOtherCandidateId(matching, consent);
            notificationService.create(otherCandidateId,
                    NotificationType.MATCH_COUNTERPART_CONSENTED, matching.getId());
            // 요청자/수신자에게도 진행 상황 알림
            notificationService.create(matching.getRequester().getId(),
                    NotificationType.MATCH_COUNTERPART_CONSENTED, matching.getId());
            notificationService.create(matching.getReceiver().getId(),
                    NotificationType.MATCH_COUNTERPART_CONSENTED, matching.getId());
        }

        return new ConsentApproveResponse(consent.getStatus().name(), matching.getStatus().name());
    }

    public void rejectConsent(String consentId, String userId) {
        MatchCandidateConsent consent = findWithAccessCheck(consentId, userId);

        MatchingStatus prevMatchingStatus = consent.getMatching().getStatus();
        consent.reject(); // PENDING 여부 엔티티 내부에서 검증
        consentRepository.save(consent);

        Matching matching = consent.getMatching();
        matching.markConsentRejected();
        matchingRepository.save(matching);

        matchLogRepository.save(MatchLog.of(matching, "CANDIDATE", userId, "DECLINED",
                prevMatchingStatus, MatchingStatus.CONSENT_REJECTED));

        // 요청자, 수신자, 상대 후보에게 실패 알림
        notificationService.create(matching.getRequester().getId(),
                NotificationType.MATCH_CONSENT_REJECTED, matching.getId());
        notificationService.create(matching.getReceiver().getId(),
                NotificationType.MATCH_CONSENT_REJECTED, matching.getId());
        String otherCandidateId = findOtherCandidateId(matching, consent);
        notificationService.create(otherCandidateId,
                NotificationType.MATCH_CONSENT_REJECTED, matching.getId());
    }

    // ────── 헬퍼 ──────

    private MatchCandidateConsent findWithAccessCheck(String consentId, String userId) {
        MatchCandidateConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new BusinessException("동의 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!consent.isOwnedBy(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return consent;
    }

    private String findOtherCandidateId(Matching matching, MatchCandidateConsent current) {
        return consentRepository.findByMatchingId(matching.getId()).stream()
                .filter(c -> !c.getId().equals(current.getId()))
                .map(c -> c.getOwnerUser().getId())
                .findFirst()
                .orElseThrow(() -> new BusinessException("상대 동의 요청을 찾을 수 없습니다.",
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void notifyAll(Matching matching, NotificationType type) {
        notificationService.create(matching.getRequester().getId(), type, matching.getId());
        notificationService.create(matching.getReceiver().getId(), type, matching.getId());
        consentRepository.findByMatchingId(matching.getId())
                .forEach(c -> notificationService.create(c.getOwnerUser().getId(), type, matching.getId()));
    }
}
