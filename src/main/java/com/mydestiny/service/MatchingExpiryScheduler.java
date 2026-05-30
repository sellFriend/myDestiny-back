package com.mydestiny.service;

import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.Matching;
import com.mydestiny.domain.MatchLog;
import com.mydestiny.domain.enums.ConsentStatus;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.repository.MatchCandidateConsentRepository;
import com.mydestiny.repository.MatchLogRepository;
import com.mydestiny.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingExpiryScheduler {

    private static final List<MatchingStatus> CONSENT_ACTIVE_STATUSES = List.of(
            MatchingStatus.CONSENT_PENDING,
            MatchingStatus.CONSENT_PARTIALLY_APPROVED
    );

    private final MatchingRepository matchingRepository;
    private final MatchCandidateConsentRepository consentRepository;
    private final MatchLogRepository matchLogRepository;
    private final NotificationService notificationService;

    // 수신자 응답 기한 초과 처리 (1분마다)
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOverdueReceiverRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<Matching> overdue = matchingRepository.findOverdueReceiverRequests(
                MatchingStatus.PENDING, now);

        if (overdue.isEmpty()) return;
        log.info("[Scheduler] 수신자 응답 기한 만료 처리: {}건", overdue.size());

        for (Matching matching : overdue) {
            matching.expire();
            matchLogRepository.save(MatchLog.of(matching, "SYSTEM", null, "EXPIRED",
                    MatchingStatus.PENDING, MatchingStatus.EXPIRED));
            notificationService.create(matching.getRequester().getId(),
                    NotificationType.MATCH_REQUEST_EXPIRED, matching.getId());
        }
    }

    // 당사자 동의 기한 초과 처리 (1분마다)
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOverdueConsentRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<Matching> overdue = matchingRepository.findOverdueConsentRequests(
                CONSENT_ACTIVE_STATUSES, ConsentStatus.PENDING, now);

        if (overdue.isEmpty()) return;
        log.info("[Scheduler] 당사자 동의 기한 만료 처리: {}건", overdue.size());

        for (Matching matching : overdue) {
            MatchingStatus prevStatus = matching.getStatus();

            // 해당 매칭의 PENDING consent 만료 처리
            consentRepository.findByMatchingId(matching.getId()).stream()
                    .filter(c -> c.getStatus() == ConsentStatus.PENDING)
                    .forEach(c -> {
                        c.expire();
                        consentRepository.save(c);
                    });

            matching.consentExpire();
            matchLogRepository.save(MatchLog.of(matching, "SYSTEM", null, "EXPIRED",
                    prevStatus, MatchingStatus.CONSENT_EXPIRED));

            notifyAll(matching, NotificationType.MATCH_CONSENT_EXPIRED);
        }
    }

    private void notifyAll(Matching matching, NotificationType type) {
        notificationService.create(matching.getRequester().getId(), type, matching.getId());
        notificationService.create(matching.getReceiver().getId(), type, matching.getId());
        consentRepository.findByMatchingId(matching.getId())
                .forEach(c -> notificationService.create(c.getOwnerUser().getId(), type, matching.getId()));
    }
}
