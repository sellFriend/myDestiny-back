-- ============================================================
-- 지인 매칭 플랫폼 DDL - MariaDB
-- V1~V9 마이그레이션 통합본
-- ============================================================

CREATE DATABASE IF NOT EXISTS my_destiny
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE my_destiny;

-- ============================================================
-- 기존 테이블 삭제 (FK 순서 역순)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS match_logs;
DROP TABLE IF EXISTS match_candidate_consents;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS blocks;
DROP TABLE IF EXISTS matchings;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS invitations;
DROP TABLE IF EXISTS phone_verifications;
DROP TABLE IF EXISTS profile_photos;
DROP TABLE IF EXISTS dating_profiles;
DROP TABLE IF EXISTS acquaintance_photos;
DROP TABLE IF EXISTS pending_invites;
DROP TABLE IF EXISTS acquaintances;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 사용자
-- V1: phone_number_*, role 추가 / email NULL 허용
-- V2: kakao_id 추가
-- V7: bio 추가
-- ============================================================

CREATE TABLE users (
    id                          CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    kakao_id                    VARCHAR(255)  NULL UNIQUE                    COMMENT '카카오 OAuth2 사용자 ID',
    email                       VARCHAR(255)  NULL UNIQUE,
    nickname                    VARCHAR(50)   NOT NULL,
    is_active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    refresh_token               VARCHAR(500),
    refresh_token_expires_at    DATETIME,
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    last_login_at               DATETIME,
    phone_number_hash           VARCHAR(128)  NULL                           COMMENT 'Argon2id 단방향 해시 (번호 일치 비교용)',
    phone_number_encrypted      VARCHAR(512)  NULL                           COMMENT 'AES-256-GCM 양방향 암호화 (마스킹 표시용)',
    phone_verified_at           DATETIME(6)   NULL                           COMMENT '전화번호 인증 완료 시각',
    role                        VARCHAR(20)   NOT NULL DEFAULT 'USER'        COMMENT 'USER | ADMIN',
    bio                         VARCHAR(500)  NULL,

    INDEX idx_users_kakao_id (kakao_id),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. 지인 프로필 (구 시스템)
-- ============================================================

CREATE TABLE acquaintances (
    id                   CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id              CHAR(36)     NOT NULL,
    name                 VARCHAR(50)  NOT NULL,
    age                  INT          NOT NULL,
    gender               ENUM('male', 'female', 'other'),
    job                  VARCHAR(100),
    intro                TEXT,
    mbti                 VARCHAR(4),
    hobbies              TEXT,
    phone_number         VARCHAR(20)  NOT NULL UNIQUE,
    email                VARCHAR(255) NOT NULL,
    kakao_id             VARCHAR(100),
    instagram_id         VARCHAR(100),
    visibility           ENUM('public', 'private') NOT NULL DEFAULT 'public',
    registration_status  ENUM('draft', 'verification_pending', 'verified') NOT NULL DEFAULT 'draft',
    verified_at          DATETIME,
    verification_token   VARCHAR(64)  UNIQUE,
    token_expires_at     DATETIME,
    token_used_at        DATETIME,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,
    deleted_at           DATETIME,

    CONSTRAINT fk_acquaintances_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_acquaintances_age
        CHECK (age BETWEEN 10 AND 99),

    CONSTRAINT chk_acquaintances_mbti
        CHECK (mbti IS NULL OR mbti REGEXP '^[A-Z]{4}$'),

    INDEX idx_acquaintances_user_id (user_id),
    INDEX idx_acquaintances_phone_number (phone_number),
    INDEX idx_acquaintances_verification_token (verification_token),
    INDEX idx_listing_query (registration_status, visibility, deleted_at),
    INDEX idx_acquaintances_mbti (mbti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 3. 초대 토큰 (구 시스템)
-- ============================================================

CREATE TABLE pending_invites (
    id           CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    user_id      CHAR(36)    NOT NULL,
    token        VARCHAR(64) NOT NULL UNIQUE,
    expires_at   DATETIME    NOT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pending_invites_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE,

    INDEX idx_pending_invites_token (token),
    INDEX idx_pending_invites_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. 지인 사진 (구 시스템)
-- ============================================================

CREATE TABLE acquaintance_photos (
    id                CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    acquaintance_id   CHAR(36) NOT NULL,
    image_url         TEXT     NOT NULL,
    display_order     INT      NOT NULL DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_acquaintance_photos_acquaintance
        FOREIGN KEY (acquaintance_id) REFERENCES acquaintances(id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_acquaintance_photos_display_order
        CHECK (display_order BETWEEN 0 AND 4),

    CONSTRAINT uq_acquaintance_photos_order
        UNIQUE (acquaintance_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. 소개 프로필 (신규 시스템)
-- V2: 최초 생성
-- V5: kakao_id, instagram_id 추가
-- V6: visibility 추가
-- V8: age, is_student, school_name, major, mbti, hobby 추가
-- V9: subject_phone_encrypted 추가
-- ============================================================

CREATE TABLE dating_profiles (
    id                      CHAR(36)      NOT NULL,
    registrant_id           CHAR(36)      NOT NULL                    COMMENT '등록자 A (users.id)',
    subject_id              CHAR(36)                                  COMMENT '승인자 B (로그인 후 연결)',
    status                  VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'    COMMENT 'ProfileStatus enum',
    name                    VARCHAR(100)  NOT NULL,
    birth_date              DATE,
    gender                  VARCHAR(10)                               COMMENT 'male | female | other',
    age                     INT           NULL                        COMMENT '나이 (직접 입력)',
    is_student              TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '학생 여부',
    school_name             VARCHAR(100)  NULL                        COMMENT '학교명 (학생인 경우)',
    major                   VARCHAR(100)  NULL                        COMMENT '학과명 (학생인 경우)',
    occupation              VARCHAR(100),
    mbti                    VARCHAR(10)   NULL                        COMMENT 'MBTI',
    hobby                   VARCHAR(200)  NULL                        COMMENT '취미',
    introduction            TEXT,
    kakao_id                VARCHAR(100)  NULL,
    instagram_id            VARCHAR(100)  NULL,
    visibility              VARCHAR(20)   NOT NULL DEFAULT 'PUBLIC',
    subject_phone_hash      VARCHAR(128)  NOT NULL                    COMMENT 'Argon2id 해시 (B 번호 일치 검증용)',
    subject_phone_encrypted VARCHAR(256)  NULL                        COMMENT 'AES-256-GCM 암호화 전화번호 (등록자 조회용)',
    is_same_person_detected TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '등록자=승인자 감지 여부',
    consent_agreed_at       DATETIME(6),
    published_at            DATETIME(6),
    rejected_at             DATETIME(6),
    rejected_reason         VARCHAR(500),
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    deleted_at              DATETIME(6),

    PRIMARY KEY (id),
    INDEX idx_dp_registrant (registrant_id),
    INDEX idx_dp_status (status),
    CONSTRAINT fk_dp_registrant FOREIGN KEY (registrant_id) REFERENCES users (id),
    CONSTRAINT fk_dp_subject    FOREIGN KEY (subject_id)    REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 6. 프로필 사진
-- ============================================================

CREATE TABLE profile_photos (
    id            CHAR(36)    NOT NULL,
    profile_id    CHAR(36)    NOT NULL,
    image_url     TEXT        NOT NULL,
    display_order INT         NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_pp_profile (profile_id),
    CONSTRAINT fk_pp_profile FOREIGN KEY (profile_id) REFERENCES dating_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 7. 초대 링크 (신규 시스템)
-- ============================================================

CREATE TABLE invitations (
    id          CHAR(36)     NOT NULL,
    profile_id  CHAR(36)     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE  COMMENT 'SHA-256(rawToken) — rawToken은 URL에만 노출',
    expires_at  DATETIME(6)  NOT NULL         COMMENT '발급 후 7일',
    used_at     DATETIME(6)                   COMMENT '사용 시각 (1회용)',
    created_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_inv_profile FOREIGN KEY (profile_id) REFERENCES dating_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 8. 전화번호 인증
-- ============================================================

CREATE TABLE phone_verifications (
    id            CHAR(36)     NOT NULL,
    user_id       CHAR(36)     NOT NULL,
    otp_hash      VARCHAR(128) NOT NULL  COMMENT 'BCrypt(otp)',
    purpose       VARCHAR(20)  NOT NULL  COMMENT 'APPROVAL',
    attempt_count INT          NOT NULL DEFAULT 0,
    expires_at    DATETIME(6)  NOT NULL  COMMENT '발급 후 5분',
    verified_at   DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_pv_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. 신고
-- ============================================================

CREATE TABLE reports (
    id          CHAR(36)    NOT NULL,
    profile_id  CHAR(36)    NOT NULL,
    reporter_id CHAR(36)    NOT NULL,
    reason      VARCHAR(30) NOT NULL  COMMENT 'CONSENT_VIOLATION|FALSE_INFO|HARASSMENT|IMPERSONATION|OTHER',
    detail      TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'  COMMENT 'PENDING|REVIEWING|RESOLVED|DISMISSED',
    created_at  DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_report_profile_reporter (profile_id, reporter_id),
    CONSTRAINT fk_rpt_profile  FOREIGN KEY (profile_id)  REFERENCES dating_profiles (id),
    CONSTRAINT fk_rpt_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 10. 매칭 요청 (V5 재설계)
-- ============================================================

CREATE TABLE matchings (
    id                    VARCHAR(36)  NOT NULL PRIMARY KEY,
    requester_id          VARCHAR(36)  NOT NULL  COMMENT '요청자(A) user_id',
    receiver_id           VARCHAR(36)  NOT NULL  COMMENT '수신자(C) user_id',
    requester_profile_id  VARCHAR(36)  NOT NULL  COMMENT '후보 B의 dating_profile_id',
    target_profile_id     VARCHAR(36)  NOT NULL  COMMENT '후보 D의 dating_profile_id',
    status                VARCHAR(40)  NOT NULL DEFAULT 'PENDING',
    message               TEXT         NULL,
    cancelled_by_id       VARCHAR(36)  NULL,
    cancel_reason         VARCHAR(500) NULL,
    reject_reason         VARCHAR(500) NULL,
    receiver_responded_at DATETIME     NULL,
    receiver_expires_at   DATETIME     NOT NULL  COMMENT '수신자 응답 기한 (+72h)',
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NOT NULL,

    CONSTRAINT fk_matching_requester         FOREIGN KEY (requester_id)         REFERENCES users(id),
    CONSTRAINT fk_matching_receiver          FOREIGN KEY (receiver_id)          REFERENCES users(id),
    CONSTRAINT fk_matching_requester_profile FOREIGN KEY (requester_profile_id) REFERENCES dating_profiles(id),
    CONSTRAINT fk_matching_target_profile    FOREIGN KEY (target_profile_id)    REFERENCES dating_profiles(id),
    CONSTRAINT fk_matching_cancelled_by      FOREIGN KEY (cancelled_by_id)      REFERENCES users(id),

    INDEX idx_matching_profile_pair (requester_profile_id, target_profile_id),
    INDEX idx_matching_requester    (requester_id, created_at),
    INDEX idx_matching_receiver     (receiver_id, created_at),
    INDEX idx_matching_status       (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 11. 매칭 동의
-- ============================================================

CREATE TABLE match_candidate_consents (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    matching_id     VARCHAR(36)  NOT NULL,
    profile_id      VARCHAR(36)  NOT NULL  COMMENT 'B 또는 D의 dating_profile_id',
    owner_user_id   VARCHAR(36)  NOT NULL  COMMENT '프로필 본인(subject) user_id',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    consent_token   VARCHAR(128) NOT NULL UNIQUE,
    consented_at    DATETIME     NULL,
    expires_at      DATETIME     NOT NULL  COMMENT '동의 기한 (+48h)',
    created_at      DATETIME     NOT NULL,

    CONSTRAINT fk_consent_matching   FOREIGN KEY (matching_id)   REFERENCES matchings(id),
    CONSTRAINT fk_consent_profile    FOREIGN KEY (profile_id)    REFERENCES dating_profiles(id),
    CONSTRAINT fk_consent_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT uq_consent_per_matching_profile UNIQUE (matching_id, profile_id),

    INDEX idx_consent_owner_status (owner_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 12. 매칭 로그
-- ============================================================

CREATE TABLE match_logs (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    matching_id     VARCHAR(36) NOT NULL,
    actor_type      VARCHAR(20) NOT NULL  COMMENT 'REQUESTER | RECEIVER | CANDIDATE | SYSTEM',
    actor_id        VARCHAR(36) NULL,
    action          VARCHAR(40) NOT NULL,
    previous_status VARCHAR(40) NULL,
    next_status     VARCHAR(40) NULL,
    created_at      DATETIME    NOT NULL,

    CONSTRAINT fk_match_log_matching FOREIGN KEY (matching_id) REFERENCES matchings(id),
    INDEX idx_match_log_matching (matching_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 13. 알림
-- V5: consent_id 추가, type ENUM 확장
-- ============================================================

CREATE TABLE notifications (
    id            CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id       CHAR(36) NOT NULL,
    type          ENUM(
        'form_submitted',
        'match_request',
        'match_accepted',
        'match_rejected',
        'verification_done',
        'acquaintance_blocked',
        'match_consent_requested',
        'match_counterpart_consented',
        'match_consent_rejected',
        'matched',
        'match_request_expired',
        'match_consent_expired'
    ) NOT NULL,
    matching_id   CHAR(36),
    consent_id    VARCHAR(36) NULL,
    payload       JSON,
    is_read       BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at       DATETIME,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    -- matching_id 는 FK 없음: form_submitted / verification_done / acquaintance_blocked
    -- 알림에서 이 컬럼을 재사용해 acquaintanceId 를 담기 때문 (matchings FK 불가)

    INDEX idx_user_unread (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 14. 지인 차단 (구 시스템)
-- ============================================================

CREATE TABLE blocks (
    id                      CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    blocker_user_id         CHAR(36) NOT NULL,
    blocked_acquaintance_id CHAR(36) NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_blocks_blocker_user
        FOREIGN KEY (blocker_user_id) REFERENCES users(id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_blocks_blocked_acquaintance
        FOREIGN KEY (blocked_acquaintance_id) REFERENCES acquaintances(id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT uq_blocks_user_acquaintance
        UNIQUE (blocker_user_id, blocked_acquaintance_id),

    INDEX idx_blocks_blocked_acquaintance_id (blocked_acquaintance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 15. 팔로우
-- ============================================================

CREATE TABLE follows (
    id           VARCHAR(36) NOT NULL PRIMARY KEY,
    follower_id  VARCHAR(36) NOT NULL  COMMENT '팔로우하는 사람',
    following_id VARCHAR(36) NOT NULL  COMMENT '팔로우 받는 사람',
    created_at   DATETIME    NOT NULL,

    CONSTRAINT fk_follow_follower  FOREIGN KEY (follower_id)  REFERENCES users(id),
    CONSTRAINT fk_follow_following FOREIGN KEY (following_id) REFERENCES users(id),
    CONSTRAINT uq_follow_pair      UNIQUE (follower_id, following_id),

    INDEX idx_follow_follower  (follower_id),
    INDEX idx_follow_following (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 16. 지인 사진 최대 5장 제한 트리거
-- ============================================================

DELIMITER //

CREATE TRIGGER trg_acquaintance_photos_max_count_before_insert
    BEFORE INSERT ON acquaintance_photos
    FOR EACH ROW
BEGIN
    DECLARE photo_count INT;

    SELECT COUNT(*)
    INTO photo_count
    FROM acquaintance_photos
    WHERE acquaintance_id = NEW.acquaintance_id;

    IF photo_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'An acquaintance can have up to 5 photos.';
    END IF;
END//

DELIMITER ;
