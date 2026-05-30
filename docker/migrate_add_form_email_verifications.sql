-- =====================================================
-- Migration: form_email_verifications 테이블 추가
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_form_email_verifications.sql
-- =====================================================

CREATE TABLE IF NOT EXISTS form_email_verifications (
    id              VARCHAR(36)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    otp_hash        VARCHAR(128) NOT NULL,
    submission_token VARCHAR(64) UNIQUE,
    attempt_count   INT          NOT NULL DEFAULT 0,
    expires_at      DATETIME     NOT NULL,
    verified_at     DATETIME,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
