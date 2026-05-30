package com.mydestiny.domain;

import com.mydestiny.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Argon2id 단방향 해시 — 번호 일치 비교용 (encoder.matches())
    @Column(name = "phone_number_hash", length = 128)
    private String phoneNumberHash;

    @Column(name = "bio", length = 500)
    private String bio;

    // AES-256-GCM 양방향 암호화 — 마스킹 표시용
    @Column(name = "phone_number_encrypted", length = 512)
    private String phoneNumberEncrypted;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "kakao_profile_image_url", length = 500)
    private String kakaoProfileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = expiresAt;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updatePhoneNumber(String phoneNumberHash, String phoneNumberEncrypted) {
        this.phoneNumberHash = phoneNumberHash;
        this.phoneNumberEncrypted = phoneNumberEncrypted;
        this.phoneVerifiedAt = LocalDateTime.now();
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateKakaoProfileImage(String url) {
        this.kakaoProfileImageUrl = url;
    }
}
