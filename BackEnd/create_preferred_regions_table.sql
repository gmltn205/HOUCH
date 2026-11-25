-- preferred_regions 테이블 생성 (최대 3개 지역 제한)
CREATE TABLE IF NOT EXISTS preferred_regions (
    profile_id BIGINT NOT NULL,
    region VARCHAR(255) NOT NULL,
    CONSTRAINT fk_preferred_regions_profile 
        FOREIGN KEY (profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT unique_profile_region UNIQUE (profile_id, region)
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_preferred_regions_profile_id ON preferred_regions(profile_id);

-- 최대 3개 지역 제한을 위한 트리거 함수 생성
CREATE OR REPLACE FUNCTION check_preferred_regions_limit()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM preferred_regions WHERE profile_id = NEW.profile_id) >= 3 THEN
        RAISE EXCEPTION '사용자는 최대 3개의 선호 지역만 설정할 수 있습니다.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
DROP TRIGGER IF EXISTS trigger_check_preferred_regions_limit ON preferred_regions;
CREATE TRIGGER trigger_check_preferred_regions_limit
    BEFORE INSERT ON preferred_regions
    FOR EACH ROW
    EXECUTE FUNCTION check_preferred_regions_limit();