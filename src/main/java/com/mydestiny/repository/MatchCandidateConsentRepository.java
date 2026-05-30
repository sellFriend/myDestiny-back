package com.mydestiny.repository;

import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.enums.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchCandidateConsentRepository extends JpaRepository<MatchCandidateConsent, String> {

    // 특정 매칭의 모든 동의 (양쪽 동의 여부 확인용)
    List<MatchCandidateConsent> findByMatchingId(String matchingId);

    // 특정 매칭 + 특정 프로필의 동의 (중복 확인, 수정용)
    Optional<MatchCandidateConsent> findByMatchingIdAndProfileId(String matchingId, String profileId);

    // 당사자 본인의 PENDING 동의 목록 (candidate consent 화면용)
    List<MatchCandidateConsent> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(
            String ownerUserId, ConsentStatus status);

    // 특정 매칭에서 주어진 상태인 동의 수 (양쪽 모두 동의했는지 확인)
    @Query("""
            SELECT COUNT(c) FROM MatchCandidateConsent c
            WHERE c.matching.id = :matchingId AND c.status = :status
            """)
    long countByMatchingIdAndStatus(
            @Param("matchingId") String matchingId,
            @Param("status") ConsentStatus status);
}
