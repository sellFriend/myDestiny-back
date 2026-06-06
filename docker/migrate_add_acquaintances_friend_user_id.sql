-- =====================================================
-- Migration: acquaintances 테이블에 friend_user_id 컬럼 추가
-- 폼을 제출한 친구(User)와 카드를 명시적으로 연결하여
-- 친구가 폼 재방문 시 자신의 기존 작성분을 찾아 수정할 수 있게 함.
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_add_acquaintances_friend_user_id.sql
-- =====================================================

ALTER TABLE acquaintances
    ADD COLUMN friend_user_id CHAR(36) NULL AFTER user_id,
    ADD CONSTRAINT fk_acquaintances_friend_user
        FOREIGN KEY (friend_user_id) REFERENCES users(id)
            ON DELETE SET NULL ON UPDATE CASCADE,
    ADD INDEX idx_acquaintances_friend_user_id (friend_user_id);
