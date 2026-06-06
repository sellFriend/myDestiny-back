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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id")
    private User friendUser;

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
        this.deletedAt = LocalDateTime.now();
    }

    // 주선자가 수정 요청 — 승인 대기 카드를 다시 DRAFT로 되돌려 친구가 수정할 수 있게 함
    public void requestEdit() {
        if (this.registrationStatus != RegistrationStatus.VERIFICATION_PENDING) {
            throw new IllegalStateException("승인 대기 상태에서만 수정 요청할 수 있습니다.");
        }
        this.registrationStatus = RegistrationStatus.DRAFT;
    }

    // 친구가 폼 재방문 후 수정 제출 — 필드 갱신 + 상태를 다시 승인 대기로 복귀
    public void updateForResubmit(
            String name, Integer age, Gender gender, String job, String intro,
            String mbti, String hobbies, String phoneNumber, String email,
            String kakaoId, String instagramId
    ) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.job = job;
        this.intro = intro;
        this.mbti = mbti;
        this.hobbies = hobbies;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.kakaoId = kakaoId;
        this.instagramId = instagramId;
        this.registrationStatus = RegistrationStatus.VERIFICATION_PENDING;
    }
}
