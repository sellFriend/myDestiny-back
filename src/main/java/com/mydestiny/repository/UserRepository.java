package com.mydestiny.repository;

import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByKakaoId(String kakaoId);

    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.id <> :currentUserId AND u.isActive = true
            AND EXISTS (
                SELECT 1 FROM DatingProfile p
                WHERE p.registrant = u AND p.status = :status
            )
            ORDER BY u.createdAt DESC
            """)
    List<User> findActiveRegistrants(@Param("status") ProfileStatus status,
                                     @Param("currentUserId") String currentUserId);
}
