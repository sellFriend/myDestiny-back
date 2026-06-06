-- =====================================================
-- Migration: dating_profiles 에 주선자 폼 흐름용 컬럼 추가
--   - upload_token: 친구 사진 업로드용 토큰
--   - subject_phone_lookup: 전화번호 중복검사용 blind index (HMAC-SHA256)
-- acquaintances → dating_profiles 통합에 따른 추가
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_dating_profiles_upload_token.sql
-- =====================================================

ALTER TABLE dating_profiles
    ADD COLUMN subject_phone_lookup VARCHAR(64) NULL
        COMMENT '전화번호 중복검사용 blind index (HMAC-SHA256)'
        AFTER subject_phone_encrypted,
    ADD COLUMN upload_token VARCHAR(64) NULL UNIQUE
        COMMENT '주선자 폼 흐름 — 친구 사진 업로드용 토큰'
        AFTER subject_phone_lookup,
    ADD INDEX idx_dp_phone_lookup (subject_phone_lookup);
