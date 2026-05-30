package com.mydestiny.repository;

import com.mydestiny.domain.Matching;
import com.mydestiny.domain.enums.MatchingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchingRepository extends JpaRepository<Matching, String> {

    // 보낸 요청 목록 (요청자 A 기준)
    List<Matching> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    // 받은 요청 목록 (수신자 C 기준)
    List<Matching> findByReceiverIdOrderByCreatedAtDesc(String receiverId);

    // 일일 요청 횟수 (V12: 5건 한도)
    int countByRequesterIdAndCreatedAtBetween(String requesterId, LocalDateTime start, LocalDateTime end);

    // 동일 프로필 조합의 최근 거절 이력 (V6: 30일 쿨다운 — 수신자 거절 + 당사자 거절 모두 포함)
    @Query("""
            SELECT COUNT(m) > 0 FROM Matching m
            WHERE m.requesterProfile.id = :requesterProfileId
              AND m.targetProfile.id = :targetProfileId
              AND m.status IN :statuses
              AND m.updatedAt > :since
            """)
    boolean existsRecentRejection(
            @Param("requesterProfileId") String requesterProfileId,
            @Param("targetProfileId") String targetProfileId,
            @Param("statuses") List<MatchingStatus> statuses,
            @Param("since") LocalDateTime since);

    // 프로필이 현재 활성 매칭에 참여 중인지 (requester 또는 target 역할 모두 포함)
    @Query("""
            SELECT COUNT(m) > 0 FROM Matching m
            WHERE (m.requesterProfile.id = :profileId OR m.targetProfile.id = :profileId)
              AND m.status IN :activeStatuses
            """)
    boolean isProfileInActiveMatch(
            @Param("profileId") String profileId,
            @Param("activeStatuses") List<MatchingStatus> activeStatuses);

    // 동일 프로필 조합에 활성 요청 존재 여부 (V5: 중복 방지)
    @Query("""
            SELECT COUNT(m) > 0 FROM Matching m
            WHERE m.requesterProfile.id = :requesterProfileId
              AND m.targetProfile.id = :targetProfileId
              AND m.status IN :activeStatuses
            """)
    boolean existsActiveMatchBetween(
            @Param("requesterProfileId") String requesterProfileId,
            @Param("targetProfileId") String targetProfileId,
            @Param("activeStatuses") List<MatchingStatus> activeStatuses);

    // MATCHED 상태 매칭 목록 (요청자 또는 수신자)
    @Query("""
            SELECT m FROM Matching m
            WHERE m.status = :status
              AND (m.requester.id = :userId OR m.receiver.id = :userId)
            ORDER BY m.updatedAt DESC
            """)
    List<Matching> findMatchedByUserId(
            @Param("userId") String userId,
            @Param("status") MatchingStatus status);

    // 만료 스케줄러: 수신자 응답 기한 초과된 PENDING 요청
    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.receiverExpiresAt < :now")
    List<Matching> findOverdueReceiverRequests(
            @Param("status") MatchingStatus status,
            @Param("now") LocalDateTime now);

    List<Matching> findAllByOrderByCreatedAtDesc();

    // 만료 스케줄러: 동의 기한 초과된 요청
    @Query("""
            SELECT DISTINCT m FROM Matching m
            JOIN MatchCandidateConsent c ON c.matching.id = m.id
            WHERE m.status IN :activeStatuses
              AND c.status = :consentStatus
              AND c.expiresAt < :now
            """)
    List<Matching> findOverdueConsentRequests(
            @Param("activeStatuses") List<MatchingStatus> activeStatuses,
            @Param("consentStatus") com.mydestiny.domain.enums.ConsentStatus consentStatus,
            @Param("now") LocalDateTime now);
}
