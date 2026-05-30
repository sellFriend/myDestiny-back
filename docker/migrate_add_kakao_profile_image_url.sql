-- =====================================================
-- Migration: users 테이블에 kakao_profile_image_url 컬럼 추가
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_kakao_profile_image_url.sql
-- =====================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS kakao_profile_image_url VARCHAR(500) NULL;
