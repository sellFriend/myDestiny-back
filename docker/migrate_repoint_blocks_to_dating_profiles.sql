-- =====================================================
-- Migration: blocks.blocked_acquaintance_id FK 를 dating_profiles 로 repoint
-- 차단 대상이 구 acquaintances → 신 dating_profiles 로 통합됨
-- 기존 블록 데이터는 구 acquaintance id 를 가리키므로 무효 → 정리 후 FK 재생성
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_repoint_blocks_to_dating_profiles.sql
-- =====================================================

-- 1) 기존 FK 제거
ALTER TABLE blocks DROP FOREIGN KEY fk_blocks_blocked_acquaintance;

-- 2) 구 acquaintance id 를 가리키는 무효 블록 데이터 정리 (데이터 이관 불필요 합의)
DELETE FROM blocks;

-- 3) dating_profiles 를 가리키도록 FK 재생성
ALTER TABLE blocks
    ADD CONSTRAINT fk_blocks_blocked_acquaintance
        FOREIGN KEY (blocked_acquaintance_id) REFERENCES dating_profiles(id)
            ON DELETE CASCADE ON UPDATE CASCADE;
