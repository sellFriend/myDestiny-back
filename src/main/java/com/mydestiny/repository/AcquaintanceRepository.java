package com.mydestiny.repository;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.enums.RegistrationStatus;
import com.mydestiny.domain.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcquaintanceRepository extends JpaRepository<Acquaintance, String> {

    Optional<Acquaintance> findByVerificationToken(String token);

    Optional<Acquaintance> findByUserIdAndFriendUserIdAndDeletedAtIsNull(String userId, String friendUserId);

    List<Acquaintance> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

    boolean existsByEmailAndRegistrationStatus(String email, RegistrationStatus status);

    boolean existsByPhoneNumberAndRegistrationStatus(String phoneNumber, RegistrationStatus status);

    // 해당 유저가 마담으로서 등록한 카드가 있는지 — 주선자 여부 판단
    boolean existsByUserIdAndDeletedAtIsNull(String userId);

    // 해당 유저가 어떤 마담의 지인으로든 이미 등록됐는지
    boolean existsByFriendUserIdAndDeletedAtIsNull(String friendUserId);

    @Query("""
            SELECT a FROM Acquaintance a
            WHERE a.registrationStatus = :status
              AND a.visibility = :visibility
              AND a.deletedAt IS NULL
              AND a.user.id != :userId
              AND a.id NOT IN (
                  SELECT b.blockedAcquaintanceId FROM Block b WHERE b.blockerUserId = :userId
              )
            ORDER BY RAND()
            """)
    List<Acquaintance> findAvailableCards(
            @Param("status") RegistrationStatus status,
            @Param("visibility") Visibility visibility,
            @Param("userId") String userId);
}
