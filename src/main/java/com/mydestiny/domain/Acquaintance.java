package com.mydestiny.domain;

import com.mydestiny.domain.enums.Gender;
import com.mydestiny.domain.enums.RegistrationStatus;
import com.mydestiny.domain.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "acquaintances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Acquaintance {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer age;

    private Gender gender;

    @Column(length = 100)
    private String job;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(length = 4)
    private String mbti;

    @Column(columnDefinition = "TEXT")
    private String hobbies;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column
    private String email;

    @Column(name = "kakao_id", length = 100)
    private String kakaoId;

    @Column(name = "instagram_id", length = 100)
    private String instagramId;

    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "registration_status", nullable = false)
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.VERIFICATION_PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 폼 제출 후 사진 업로드에 사용하는 토큰 (pending_invites에서 이관)
    @Column(name = "verification_token", unique = true, length = 64)
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "token_used_at")
    private LocalDateTime tokenUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "acquaintance", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<AcquaintancePhoto> photos = new ArrayList<>();

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

    public void approve() {
        this.registrationStatus = RegistrationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.tokenUsedAt = LocalDateTime.now();
    }

    public void reject() {
        this.registrationStatus = RegistrationStatus.DRAFT;
    }
}
