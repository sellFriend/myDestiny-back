package com.mydestiny.service;

import com.mydestiny.domain.*;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.matching.MatchingRequest;
import com.mydestiny.dto.matching.MatchingResponse;
import com.mydestiny.dto.matching.ContactResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.MatchCandidateConsentRepository;
import com.mydestiny.repository.MatchLogRepository;
import com.mydestiny.repository.MatchingRepository;
import com.mydestiny.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    private static final int DAILY_LIMIT = 5;
    private static final int RECEIVER_EXPIRES_HOURS = 72;
    private static final int CONSENT_EXPIRES_HOURS = 48;

    private static final List<MatchingStatus> ACTIVE_STATUSES = List.of(
            MatchingStatus.PENDING,
            MatchingStatus.ACCEPTED_BY_RECEIVER,
            MatchingStatus.CONSENT_PENDING,
            MatchingStatus.CONSENT_PARTIALLY_APPROVED
    );

    // 프로필 점유 상태: 진행 중 매칭 + 성사된 매칭 (한 프로필은 동시에 하나의 매칭만)
    private static final List<MatchingStatus> OCCUPIED_STATUSES = MatchingStatus.OCCUPIED;

    // 30일 쿨다운 적용 대상: 수신자 거절 + 당사자 거절 모두 포함
    private static final List<MatchingStatus> COOLDOWN_STATUSES = List.of(
            MatchingStatus.REJECTED_BY_RECEIVER,
            MatchingStatus.CONSENT_REJECTED
    );

    private final MatchingRepository matchingRepository;
    private final MatchCandidateConsentRepository consentRepository;
    private final MatchLogRepository matchLogRepository;
    private final DatingProfileRepository profileRepository;
    private final NotificationService notificationService;

    public MatchingResponse createMatching(String userId, MatchingRequest req) {
        DatingProfile requesterProfile = profileRepository.findById(req.requesterProfileId())
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // V3: 내 프로필인지 확인
        if (!requesterProfile.getRegistrant().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        // V2: 내 프로필 공개 상태 확인
        if (requesterProfile.getStatus() != ProfileStatus.PUBLISHED) {
            throw new BusinessException("공개된 프로필만 매칭 요청이 가능합니다.", HttpStatus.BAD_REQUEST);
        }

        DatingProfile targetProfile = profileRepository.findById(req.targetProfileId())
                .orElseThrow(() -> new BusinessException("대상 프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        // V1: 상대 프로필 공개 상태 확인
        if (targetProfile.getStatus() != ProfileStatus.PUBLISHED) {
            throw new BusinessException("매칭 요청이 불가한 프로필입니다.", HttpStatus.BAD_REQUEST);
        }

        User requester = requesterProfile.getRegistrant();
        User receiver = targetProfile.getRegistrant();

        // V4: 자기 자신 프로필에 요청 불가
        if (receiver.getId().equals(userId)) {
            throw new BusinessException("자신의 프로필에 매칭 요청을 보낼 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        // V7: 계정 활성 상태 확인
        if (!requester.isActive() || !receiver.isActive()) {
            throw new BusinessException("정지된 계정입니다.", HttpStatus.FORBIDDEN);
        }
        // V5: 동일 프로필 조합 활성 매칭 중복 방지
        if (matchingRepository.existsActiveMatchBetween(
                req.requesterProfileId(), req.targetProfileId(), ACTIVE_STATUSES)) {
            throw new BusinessException("이미 진행 중인 매칭 요청이 있습니다.", HttpStatus.CONFLICT);
        }
        // 두 프로필 중 하나라도 이미 매칭에 점유(성사·확정 진행)된 경우 요청 불가
        if (matchingRepository.isProfileInActiveMatch(req.requesterProfileId(), OCCUPIED_STATUSES)) {
            throw new BusinessException("내 친구 프로필이 이미 다른 매칭에 참여 중입니다.", HttpStatus.CONFLICT);
        }
        if (matchingRepository.isProfileInActiveMatch(req.targetProfileId(), OCCUPIED_STATUSES)) {
            throw new BusinessException("상대 친구 프로필이 이미 다른 매칭에 참여 중입니다.", HttpStatus.CONFLICT);
        }
        // 보낸 요청 1건 제한: 내 친구 프로필은 한 번에 한 명에게만 요청 가능 (A→B, A→C 차단)
        if (matchingRepository.existsByRequesterProfileIdAndStatus(
                req.requesterProfileId(), MatchingStatus.PENDING)) {
            throw new BusinessException(
                    "내 친구 프로필이 이미 다른 매칭 요청을 보낸 상태입니다. 한 번에 한 명에게만 요청할 수 있습니다.",
                    HttpStatus.CONFLICT);
        }
        // V6: 30일 쿨다운 (수신자 거절 + 당사자 거절 모두 포함)
        if (matchingRepository.existsRecentRejection(
                req.requesterProfileId(), req.targetProfileId(),
                COOLDOWN_STATUSES, LocalDateTime.now().minusDays(30))) {
            throw new BusinessException("최근 30일 내 거절된 조합입니다. 30일 후 재요청 가능합니다.", HttpStatus.CONFLICT);
        }
        // TODO: 테스트 완료 후 아래 한도 검사 재활성화
        // LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        // if (matchingRepository.countByRequesterIdAndCreatedAtBetween(
        //         userId, todayStart, todayStart.plusDays(1)) >= DAILY_LIMIT) {
        //     throw new BusinessException("하루 매칭 요청 한도(" + DAILY_LIMIT + "건)를 초과했습니다.",
        //             HttpStatus.TOO_MANY_REQUESTS);
        // }

        Matching matching = matchingRepository.save(Matching.builder()
                .requester(requester)
                .receiver(receiver)
                .requesterProfile(requesterProfile)
                .targetProfile(targetProfile)
                .message(req.message())
                .receiverExpiresAt(LocalDateTime.now().plusHours(RECEIVER_EXPIRES_HOURS))
                .build());

        matchLogRepository.save(MatchLog.of(matching, "REQUESTER", userId,
                "REQUESTED", null, MatchingStatus.PENDING));
        notificationService.create(receiver.getId(), NotificationType.MATCH_REQUEST, matching.getId());

        return MatchingResponse.from(matching);
    }

    @Transactional(readOnly = true)
    public List<MatchingResponse> getSentMatchings(String userId) {
        return matchingRepository.findByRequesterIdOrderByCreatedAtDesc(userId)
                .stream().map(MatchingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchingResponse> getReceivedMatchings(String userId) {
        return matchingRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream().map(MatchingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MatchingResponse getMatchingDetail(String matchingId, String userId) {
        return MatchingResponse.from(findWithAccessCheck(matchingId, userId));
    }

    public MatchingResponse acceptMatching(String matchingId, String userId) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!matching.getReceiver().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (matching.getStatus() != MatchingStatus.PENDING) {
            throw new BusinessException("이미 처리된 매칭입니다.", HttpStatus.CONFLICT);
        }
        if (matching.isReceiverResponseOverdue()) {
            matching.expire();
            throw new BusinessException("응답 기한이 만료된 요청입니다.", HttpStatus.GONE);
        }

        matching.matchDirectlyByReceiver();
        matchingRepository.save(matching);

        matchLogRepository.save(MatchLog.of(matching, "RECEIVER", userId, "ACCEPTED",
                MatchingStatus.PENDING, MatchingStatus.MATCHED));

        // 성사된 두 프로필에 엮인 다른 모든 활성 요청(보낸·받은)을 자동 취소
        cancelEntangledMatches(matching, userId);

        // 요청자(A)와 수신자(C) 모두에게 매칭 성사 알림
        notificationService.create(matching.getRequester().getId(),
                NotificationType.MATCHED, matching.getId());
        notificationService.create(matching.getReceiver().getId(),
                NotificationType.MATCHED, matching.getId());

        return MatchingResponse.from(matching);
    }

    public MatchingResponse rejectMatching(String matchingId, String userId, String reason) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!matching.getReceiver().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (matching.getStatus() != MatchingStatus.PENDING) {
            throw new BusinessException("이미 처리된 매칭입니다.", HttpStatus.CONFLICT);
        }

        matching.rejectByReceiver(reason);
        matchLogRepository.save(MatchLog.of(matching, "RECEIVER", userId, "REJECTED",
                MatchingStatus.PENDING, MatchingStatus.REJECTED_BY_RECEIVER));
        notificationService.create(matching.getRequester().getId(),
                NotificationType.MATCH_REJECTED, matching.getId());

        return MatchingResponse.from(matching);
    }

    public void cancelMatching(String matchingId, String userId) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!matching.getRequester().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        matching.cancel(matching.getRequester(), null);
        matchLogRepository.save(MatchLog.of(matching, "REQUESTER", userId, "CANCELLED",
                MatchingStatus.PENDING, MatchingStatus.CANCELLED));
    }

    // 성사(MATCHED)된 매칭을 당사자(요청자 또는 수신자)가 취소
    public MatchingResponse cancelMatchedMatching(String matchingId, String userId, String reason) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        boolean isRequester = matching.getRequester().getId().equals(userId);
        boolean isReceiver = matching.getReceiver().getId().equals(userId);
        if (!isRequester && !isReceiver) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (matching.getStatus() != MatchingStatus.MATCHED) {
            throw new BusinessException("성사된 매칭만 취소할 수 있습니다.", HttpStatus.CONFLICT);
        }

        User actor = isRequester ? matching.getRequester() : matching.getReceiver();
        matching.cancelAfterMatch(actor, reason);
        matchingRepository.save(matching);

        matchLogRepository.save(MatchLog.of(matching,
                isRequester ? "REQUESTER" : "RECEIVER", userId, "CANCELLED_AFTER_MATCH",
                MatchingStatus.MATCHED, MatchingStatus.CANCELLED_AFTER_MATCH));

        // 취소한 본인을 제외한 상대방에게 매칭 해제 알림
        String counterpartId = isRequester
                ? matching.getReceiver().getId()
                : matching.getRequester().getId();
        notificationService.create(counterpartId, NotificationType.MATCH_RELEASED, matching.getId());

        return MatchingResponse.from(matching);
    }

    @Transactional(readOnly = true)
    public List<MatchingResponse> getMatchedMatchings(String userId) {
        return matchingRepository.findMatchedByUserId(userId, MatchingStatus.MATCHED)
                .stream().map(MatchingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(String matchingId, String userId) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (matching.getStatus() != MatchingStatus.MATCHED) {
            throw new BusinessException("성사된 매칭만 연락처를 조회할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        // 접근 권한 및 어느 쪽 연락처를 반환할지 결정
        // 요청자 측(A 또는 B) → D의 연락처, 수신자 측(C 또는 D) → B의 연락처
        User subjectB = matching.getRequesterProfile().getSubject();
        User subjectD = matching.getTargetProfile().getSubject();

        boolean isRequesterSide = matching.getRequester().getId().equals(userId)
                || (subjectB != null && subjectB.getId().equals(userId));
        boolean isReceiverSide = matching.getReceiver().getId().equals(userId)
                || (subjectD != null && subjectD.getId().equals(userId));

        if (!isRequesterSide && !isReceiverSide) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        DatingProfile contactProfile = isRequesterSide
                ? matching.getTargetProfile()    // D의 연락처
                : matching.getRequesterProfile(); // B의 연락처

        return ContactResponse.from(contactProfile);
    }

    // ────── 헬퍼 ──────

    // 성사된 매칭(matched)의 두 프로필에 엮인 다른 활성 요청을 모두 자동 취소
    private void cancelEntangledMatches(Matching matched, String actorId) {
        List<String> profileIds = List.of(
                matched.getRequesterProfile().getId(),
                matched.getTargetProfile().getId());

        List<Matching> entangled = matchingRepository.findActiveByProfiles(
                matched.getId(), profileIds, ACTIVE_STATUSES);

        for (Matching m : entangled) {
            MatchingStatus prev = m.getStatus();
            m.autoCancel("다른 매칭 성사로 자동 취소되었습니다.");
            matchingRepository.save(m);

            matchLogRepository.save(MatchLog.of(m, "SYSTEM", actorId, "AUTO_CANCELLED",
                    prev, MatchingStatus.CANCELLED));

            notificationService.create(m.getRequester().getId(),
                    NotificationType.MATCH_CANCELLED, m.getId());
            notificationService.create(m.getReceiver().getId(),
                    NotificationType.MATCH_CANCELLED, m.getId());
        }
    }

    private Matching findWithAccessCheck(String matchingId, String userId) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BusinessException("매칭을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        boolean isParty = matching.getRequester().getId().equals(userId)
                || matching.getReceiver().getId().equals(userId);
        if (!isParty) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return matching;
    }

    private User requireSubject(DatingProfile profile) {
        User subject = profile.getSubject();
        if (subject == null) {
            throw new BusinessException("프로필 당사자 계정이 연결되지 않았습니다.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return subject;
    }
}
