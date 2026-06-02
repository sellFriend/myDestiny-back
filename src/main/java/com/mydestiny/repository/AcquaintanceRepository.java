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

    List<Acquaintance> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

    boolean existsByEmailAndRegistrationStatus(String email, RegistrationStatus status);

    boolean existsByPhoneNumberAndRegistrationStatus(String phoneNumber, RegistrationStatus status);

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
