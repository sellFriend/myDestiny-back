-- =====================================================
-- Migration: 구 acquaintances 시스템 테이블 제거
-- 주선자 폼/카드/차단 흐름이 dating_profiles 로 통합 완료된 뒤 실행
-- 선행 조건: migrate_repoint_blocks_to_dating_profiles.sql 먼저 적용 (blocks FK 제거 필요)
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_drop_acquaintances_tables.sql
-- =====================================================

DROP TRIGGER IF EXISTS trg_acquaintance_photos_max_count_before_insert;

DROP TABLE IF EXISTS acquaintance_photos;
DROP TABLE IF EXISTS acquaintances;
