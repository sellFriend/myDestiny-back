package com.mydestiny.domain;

import com.mydestiny.domain.enums.Gender;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dating_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DatingProfile {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id", nullable = false)
    private User registrant;

    // B가 로그인 후 연결됨 (초대 전까지 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private User subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProfileStatus status = ProfileStatus.DRAFT;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Gender.GenderConverter autoApply=true → DB에 "MALE"/"FEMALE"/"OTHER"로 저장
    private Gender gender;

    @Column
    private Integer age;

    @Column(name = "is_student", nullable = false)
    @Builder.Default
    private boolean isStudent = false;

    @Column(name = "school_name", length = 100)
    private String schoolName;

    @Column(length = 100)
    private String major;

    @Column(length = 100)
    private String occupation;

    @Column(length = 10)
    private String mbti;

    @Column(length = 200)
    private String hobby;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "kakao_id", length = 100)
    private String kakaoId;

    @Column(name = "instagram_id", length = 100)
    private String instagramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    // A가 입력한 B의 전화번호 — Argon2id 단방향 해시 (검증용)
    @Column(name = "subject_phone_hash", nullable = false, length = 128)
    private String subjectPhoneHash;

    // A가 입력한 B의 전화번호 — AES-256-GCM 암호화 (등록자 조회용)
    @Column(name = "subject_phone_encrypted", length = 256)
    private String subjectPhoneEncrypted;

    // 전화번호 중복검사용 blind index — 고정키 HMAC-SHA256 (동등비교 조회용)
    @Column(name = "subject_phone_lookup", length = 64)
    private String subjectPhoneLookup;

    @Column(name = "is_same_person_detected", nullable = false)
    @Builder.Default
    private boolean isSamePersonDetected = false;

    // 주선자 폼 흐름에서 친구(subject)가 사진을 업로드할 때 쓰는 토큰
    @Column(name = "upload_token", unique = true, length = 64)
    private String uploadToken;

    @Column(name = "consent_agreed_at")
    private LocalDateTime consentAgreedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "profile", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfilePhoto> photos = new ArrayList<>();

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

    // null 필드는 기존 값 유지 (PATCH 의미)
    public void updateDraft(String name, Integer age, String gender, Boolean isStudent,
                            String schoolName, String major, String occupation,
                            String mbti, String hobby, String introduction,
                            String kakaoId, String instagramId) {
        if (this.status != ProfileStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 수정 가능합니다.");
        }
        applyUpdate(name, age, gender, isStudent, schoolName, major, occupation, mbti, hobby, introduction, kakaoId, instagramId);
    }

    // 등록자가 어느 상태에서든 수정 → DRAFT로 초기화 (친구 재승인 필요)
    public void updateByRegistrant(String name, Integer age, String gender, Boolean isStudent,
                                   String schoolName, String major, String occupation,
                                   String mbti, String hobby, String introduction,
                                   String kakaoId, String instagramId) {
        if (this.status == ProfileStatus.DELETED) {
            throw new IllegalStateException("삭제된 프로필은 수정할 수 없습니다.");
        }
        applyUpdate(name, age, gender, isStudent, schoolName, major, occupation, mbti, hobby, introduction, kakaoId, instagramId);
        this.status = ProfileStatus.DRAFT;
        this.publishedAt = null;
    }

    // 공개 범위만 단독 변경 (재승인 불필요)
    public void changeVisibility(ProfileVisibility visibility) {
        this.visibility = visibility;
    }

    // B(subject)가 PENDING_APPROVAL / REVIEW_REQUIRED 상태에서 수정
    public void updateBySubject(String name, Integer age, String gender, Boolean isStudent,
                                String schoolName, String major, String occupation,
                                String mbti, String hobby, String introduction,
                                String kakaoId, String instagramId) {
        if (this.status != ProfileStatus.PENDING_APPROVAL
                && this.status != ProfileStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("확인 대기 상태에서만 수정 가능합니다.");
        }
        applyUpdate(name, age, gender, isStudent, schoolName, major, occupation, mbti, hobby, introduction, kakaoId, instagramId);
    }

    private void applyUpdate(String name, Integer age, String gender, Boolean isStudent,
                             String schoolName, String major, String occupation,
                             String mbti, String hobby, String introduction,
                             String kakaoId, String instagramId) {
        if (name != null) this.name = name;
        if (age != null) this.age = age;
        if (gender != null) this.gender = Gender.fromDb(gender);
        if (isStudent != null) this.isStudent = isStudent;
        if (schoolName != null) this.schoolName = schoolName;
        if (major != null) this.major = major;
        if (occupation != null) this.occupation = occupation;
        if (mbti != null) this.mbti = mbti;
        if (hobby != null) this.hobby = hobby;
        if (introduction != null) this.introduction = introduction;
        if (kakaoId != null) this.kakaoId = kakaoId;
        if (instagramId != null) this.instagramId = instagramId;
    }

    public void changeStatus(ProfileStatus newStatus) {
        this.status = newStatus;
    }

    public void recordConsent() {
        this.consentAgreedAt = LocalDateTime.now();
    }

    public void approve() {
        if (this.status != ProfileStatus.PENDING_APPROVAL
                && this.status != ProfileStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 또는 PENDING_APPROVAL 상태에서만 승인할 수 있습니다.");
        }
        this.status = ProfileStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (this.status != ProfileStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("PENDING_APPROVAL 상태에서만 거절할 수 있습니다.");
        }
        this.status = ProfileStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedReason = reason;
    }

    public void adminApprove() {
        if (this.status != ProfileStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("REVIEW_REQUIRED 상태에서만 관리자 승인이 가능합니다.");
        }
        this.status = ProfileStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void adminReject(String reason) {
        if (this.status != ProfileStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("REVIEW_REQUIRED 상태에서만 관리자 거절이 가능합니다.");
        }
        this.status = ProfileStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedReason = reason;
    }

    public void suspend() {
        this.status = ProfileStatus.SUSPENDED;
    }

    public void markReported() {
        this.status = ProfileStatus.REPORTED;
    }

    public boolean isSubject(String userId) {
        return this.subject != null && this.subject.getId().equals(userId);
    }

    // B가 번호 인증 완료 후 subject 연결 + 동일인 감지
    public void linkSubject(User subject, boolean isSamePerson) {
        this.subject = subject;
        this.isSamePersonDetected = isSamePerson;
        this.status = isSamePerson ? ProfileStatus.REVIEW_REQUIRED : ProfileStatus.PENDING_APPROVAL;
    }

    public void softDelete() {
        this.status = ProfileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // === 주선자 폼 흐름 (친구가 본인 정보를 작성, 주선자가 승인) ===

    // 주선자가 수정 요청 — 승인 대기/공개된 카드를 DRAFT로 되돌려 친구가 다시 작성하게 함
    public void requestEditByRegistrant() {
        if (this.status != ProfileStatus.PENDING_APPROVAL
                && this.status != ProfileStatus.PUBLISHED) {
            throw new IllegalStateException("승인 대기 또는 공개 상태에서만 수정 요청할 수 있습니다.");
        }
        this.status = ProfileStatus.DRAFT;
    }

    // 주선자가 거절 — 승인 대기 상태에서만 가능 (이미 승인/매칭된 카드는 거절 불가)
    public void rejectByRegistrant() {
        if (this.status != ProfileStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("승인 대기 상태에서만 거절할 수 있습니다.");
        }
        this.status = ProfileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // 친구가 폼 재방문 후 수정 제출 — 필드 갱신 + 상태를 다시 승인 대기로 복귀
    public void resubmitByFriend(String name, Integer age, String gender,
                                 boolean isStudent, String schoolName, String major, String occupation,
                                 String introduction, String mbti, String hobby,
                                 String kakaoId, String instagramId,
                                 String subjectPhoneHash, String subjectPhoneEncrypted,
                                 String subjectPhoneLookup) {
        this.name = name;
        this.age = age;
        this.gender = gender != null ? Gender.fromDb(gender) : null;
        this.isStudent = isStudent;
        this.schoolName = schoolName;
        this.major = major;
        this.occupation = occupation;
        this.introduction = introduction;
        this.mbti = mbti;
        this.hobby = hobby;
        this.kakaoId = kakaoId;
        this.instagramId = instagramId;
        this.subjectPhoneHash = subjectPhoneHash;
        this.subjectPhoneEncrypted = subjectPhoneEncrypted;
        this.subjectPhoneLookup = subjectPhoneLookup;
        this.status = ProfileStatus.PENDING_APPROVAL;
    }

    public boolean isOwnedBy(String userId) {
        return this.registrant.getId().equals(userId);
    }
}
