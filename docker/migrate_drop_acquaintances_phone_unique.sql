-- =====================================================
-- Migration: acquaintances.phone_number UNIQUE 제약 제거
-- 중복 체크는 서비스 레이어(VERIFIED 상태 기준)에서만 수행
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_drop_acquaintances_phone_unique.sql
-- =====================================================

ALTER TABLE acquaintances DROP INDEX phone_number;
