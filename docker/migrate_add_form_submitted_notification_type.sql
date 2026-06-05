-- =====================================================
-- Migration: notifications.type ENUM에 'form_submitted' 값 추가
-- (FORM_SUBMITTED 알림 INSERT 시 "Data truncated for column 'type'" 오류 해결)
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_form_submitted_notification_type.sql
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
        'match_consent_expired'
    ) NOT NULL;
