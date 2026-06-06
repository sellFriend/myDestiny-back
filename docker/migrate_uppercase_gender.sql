-- =====================================================
-- Migration: dating_profiles.gender 값을 대문자로 통일
--   - 기존: 'male' | 'female' | 'other' (소문자)
--   - 변경: 'MALE' | 'FEMALE' | 'OTHER' (대문자)
-- Gender enum dbValue 를 대문자로 바꾼 것에 맞춰 기존 데이터 정규화.
-- 실행: docker exec -i my-mariadb mariadb -u root -proot my_destiny < docker/migrate_uppercase_gender.sql
-- =====================================================

UPDATE dating_profiles
    SET gender = UPPER(gender)
    WHERE gender IS NOT NULL;

ALTER TABLE dating_profiles
    MODIFY COLUMN gender VARCHAR(10) COMMENT 'MALE | FEMALE | OTHER';
