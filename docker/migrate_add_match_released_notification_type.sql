-- =====================================================
-- Migration: notifications.type ENUM 전체 재정의
-- 성사된 매칭이 당사자에 의해 취소될 때 발송하는 'match_released' 알림 타입 추가.
-- 직전 edit_requested 마이그레이션이 ENUM을 재정의하면서 'match_cancelled'를
-- 누락시켰으므로 함께 복원한다. (코드 NotificationType enum 과 1:1 일치)
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_match_released_notification_type.sql
-- =====================================================

ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
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
        'match_consent_expired',
        'match_cancelled',
        'match_released',
        'edit_requested'
    ) NOT NULL;
