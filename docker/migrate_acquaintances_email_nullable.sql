-- =====================================================
-- Migration: acquaintances.email NULL 허용으로 변경
-- 카카오 로그인 기반 폼 제출로 전환 — 이메일은 카카오 계정에서 자동 수집
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_acquaintances_email_nullable.sql
-- =====================================================

ALTER TABLE acquaintances
    MODIFY COLUMN email VARCHAR(255) NULL;
