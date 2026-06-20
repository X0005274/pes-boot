-- =====================================================================
-- Hub 적재 워터마크 커서 (HIS 기반 증분 replay 의 재시작 이어가기)
-- =====================================================================
CREATE TABLE PES_HUB_CURSOR (
    CURSOR_KEY VARCHAR2(60 CHAR)  NOT NULL,
    WATERMARK  VARCHAR2(40 CHAR),
    UPDATE_TM  VARCHAR2(20 CHAR),
    CONSTRAINT PK_PES_HUB_CURSOR PRIMARY KEY (CURSOR_KEY)
);
