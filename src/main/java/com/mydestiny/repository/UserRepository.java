package com.mydestiny.repository;

import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByKakaoId(String kakaoId);

    /**
     * 로그인 시 마지막 로그인 시각/프로필 이미지를 단일 원자 UPDATE로 갱신한다.
     * read-then-write 창을 없애 동시 로그인 시 'Record has changed' 충돌을 방지한다.
     * profileImageUrl 이 null 이면 기존 값을 유지한다.
     */
    @Modifying
    @Query("""
            UPDATE User u
            SET u.lastLoginAt = :now,
                u.kakaoProfileImageUrl = COALESCE(:profileImageUrl, u.kakaoProfileImageUrl),
                u.updatedAt = :now
            WHERE u.id = :id
            """)
    int touchLogin(@Param("id") String id,
                   @Param("profileImageUrl") String profileImageUrl,
                   @Param("now") LocalDateTime now);

    /**
     * 리프레시 토큰을 단일 원자 UPDATE로 갱신한다.
     * touchLogin 과 동일하게 read-then-write 창을 없애 동시 로그인 시 'Record has changed' 충돌을 방지한다.
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE User u
            SET u.refreshToken = :refreshToken,
                u.refreshTokenExpiresAt = :expiresAt,
                u.updatedAt = :now
            WHERE u.id = :id
            """)
    int updateRefreshToken(@Param("id") String id,
                           @Param("refreshToken") String refreshToken,
                           @Param("expiresAt") LocalDateTime expiresAt,
                           @Param("now") LocalDateTime now);

    @Query("SELECT u.kakaoProfileImageUrl FROM User u WHERE u.id = :id")
    Optional<String> findKakaoProfileImageUrlById(@Param("id") String id);

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
