-- =====================================================
-- Migration: notifications.matching_id 의 FK 제약 제거
-- (form_submitted / verification_done / acquaintance_blocked 알림은
--  matching_id 컬럼을 재사용해 acquaintanceId 를 담는데,
--  fk_notifications_matching FK 때문에 INSERT 시 1452 FK 위반 → 500 발생)
-- 명세서(api-spec-actions.md): "matchingId 필드에 acquaintanceId 값이 담김 (같은 컬럼 재사용)"
-- 실행(앱이 연결하는 원격 DB 대상): mariadb -h 138.2.98.160 -P 3307 -u root -proot my_destiny < docker/migrate_drop_notifications_matching_fk.sql
-- =====================================================

ALTER TABLE notifications
    DROP FOREIGN KEY fk_notifications_matching;
