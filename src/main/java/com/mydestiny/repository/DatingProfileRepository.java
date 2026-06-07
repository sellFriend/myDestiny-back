package com.mydestiny.repository;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.ProfileVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatingProfileRepository extends JpaRepository<DatingProfile, String> {

    List<DatingProfile> findByRegistrantIdAndStatusNotOrderByCreatedAtDesc(
            String registrantId, ProfileStatus status);

    Optional<DatingProfile> findByIdAndStatusNot(String id, ProfileStatus status);

    List<DatingProfile> findByStatusOrderByPublishedAtDesc(ProfileStatus status);

    long countByRegistrantIdAndStatus(String registrantId, ProfileStatus status);

    // === 주선자 폼 흐름 ===

    Optional<DatingProfile> findByUploadToken(String uploadToken);

    // 같은 주선자가 같은 친구로 작성 중인(삭제 안 된) 카드
    Optional<DatingProfile> findByRegistrantIdAndSubjectIdAndStatusNot(
            String registrantId, String subjectId, ProfileStatus status);

    // 해당 유저가 어떤 주선자의 친구(매물)로든 이미 등록됐는지
    boolean existsBySubjectIdAndStatusNot(String subjectId, ProfileStatus status);

    // 해당 유저가 주선자로서 등록한 카드가 있는지
    boolean existsByRegistrantIdAndStatusNot(String registrantId, ProfileStatus status);

    // 같은 전화번호(blind index)로 이미 승인 완료된 프로필이 있는지 — 번호 중복 차단
    boolean existsBySubjectPhoneLookupAndStatus(String subjectPhoneLookup, ProfileStatus status);

    // 카드 목록 — 공개된 프로필 중 본인 관련/차단/매칭 성사 제외
    @Query("""
            SELECT p FROM DatingProfile p
            WHERE p.status = :status
              AND p.visibility = :visibility
              AND p.deletedAt IS NULL
              AND p.registrant.id != :userId
              AND (p.subject IS NULL OR p.subject.id != :userId)
              AND p.id NOT IN (
                  SELECT b.blockedAcquaintanceId FROM Block b WHERE b.blockerUserId = :userId
              )
              AND NOT EXISTS (
                  SELECT 1 FROM Matching m
                  WHERE m.status = :matchedStatus
                    AND (m.requesterProfile.id = p.id OR m.targetProfile.id = p.id)
              )
            ORDER BY RAND()
            """)
    List<DatingProfile> findAvailableCards(
            @Param("status") ProfileStatus status,
            @Param("visibility") ProfileVisibility visibility,
            @Param("userId") String userId,
            @Param("matchedStatus") MatchingStatus matchedStatus);
}
