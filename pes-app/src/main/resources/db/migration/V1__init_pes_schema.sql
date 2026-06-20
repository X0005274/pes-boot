-- =====================================================================
-- PES 초기 스키마 (Oracle AI Database 26ai)
-- JPA 엔티티와 1:1 매핑. 운영은 ddl-auto=validate 로 이 스키마를 검증한다.
-- 문자열은 멀티바이트(한글) 안전을 위해 CHAR 의미 사용.
-- *_HIS PK = (도메인ID, TIMEKEY), 도메인ID 선행 컬럼으로 도메인별 이력 조회 최적화.
-- =====================================================================

-- ---------------------------------------------------------------------
-- LOT
-- ---------------------------------------------------------------------
CREATE TABLE PES_LOT_MAS (
    LOT_ID      VARCHAR2(40 CHAR)  NOT NULL,
    WF_ID       VARCHAR2(40 CHAR),
    DURABLE_ID  VARCHAR2(40 CHAR),
    STAT_TYP    VARCHAR2(20 CHAR),
    LOT_SPEC    VARCHAR2(200 CHAR),
    SRC_SYSTEM  VARCHAR2(20 CHAR),
    USER_ID     VARCHAR2(30 CHAR),
    CREATE_TM   VARCHAR2(20 CHAR),
    UPDATE_TM   VARCHAR2(20 CHAR),
    VER_NO      NUMBER(19)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PES_LOT_MAS PRIMARY KEY (LOT_ID)
);

CREATE TABLE PES_LOT_HIS (
    LOT_ID      VARCHAR2(40 CHAR)  NOT NULL,
    TIMEKEY     VARCHAR2(24 CHAR)  NOT NULL,
    METHOD      VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_CD    VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_TM    VARCHAR2(20 CHAR),
    EVENT_DESC  VARCHAR2(200 CHAR),
    STAT_TYP    VARCHAR2(20 CHAR),
    SRC_SYSTEM  VARCHAR2(20 CHAR)  NOT NULL,
    USER_ID     VARCHAR2(30 CHAR),
    CORR_ID     VARCHAR2(60 CHAR),
    REQUEST_TM  VARCHAR2(20 CHAR),
    LOCALE      VARCHAR2(20 CHAR),
    CONSTRAINT PK_PES_LOT_HIS PRIMARY KEY (LOT_ID, TIMEKEY)
);

-- ---------------------------------------------------------------------
-- WF
-- ---------------------------------------------------------------------
CREATE TABLE PES_WF_MAS (
    WF_ID       VARCHAR2(40 CHAR)  NOT NULL,
    LOT_ID      VARCHAR2(40 CHAR),
    STAT_TYP    VARCHAR2(20 CHAR),
    WF_SPEC     VARCHAR2(200 CHAR),
    SRC_SYSTEM  VARCHAR2(20 CHAR),
    USER_ID     VARCHAR2(30 CHAR),
    CREATE_TM   VARCHAR2(20 CHAR),
    UPDATE_TM   VARCHAR2(20 CHAR),
    VER_NO      NUMBER(19)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PES_WF_MAS PRIMARY KEY (WF_ID)
);

CREATE TABLE PES_WF_HIS (
    WF_ID       VARCHAR2(40 CHAR)  NOT NULL,
    TIMEKEY     VARCHAR2(24 CHAR)  NOT NULL,
    METHOD      VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_CD    VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_TM    VARCHAR2(20 CHAR),
    EVENT_DESC  VARCHAR2(200 CHAR),
    STAT_TYP    VARCHAR2(20 CHAR),
    SRC_SYSTEM  VARCHAR2(20 CHAR)  NOT NULL,
    USER_ID     VARCHAR2(30 CHAR),
    CORR_ID     VARCHAR2(60 CHAR),
    REQUEST_TM  VARCHAR2(20 CHAR),
    LOCALE      VARCHAR2(20 CHAR),
    CONSTRAINT PK_PES_WF_HIS PRIMARY KEY (WF_ID, TIMEKEY)
);

-- ---------------------------------------------------------------------
-- DURABLE
-- ---------------------------------------------------------------------
CREATE TABLE PES_DURABLE_MAS (
    DURABLE_ID   VARCHAR2(40 CHAR)  NOT NULL,
    LOT_ID       VARCHAR2(40 CHAR),
    STAT_TYP     VARCHAR2(20 CHAR),
    DURABLE_SPEC VARCHAR2(200 CHAR),
    SRC_SYSTEM   VARCHAR2(20 CHAR),
    USER_ID      VARCHAR2(30 CHAR),
    CREATE_TM    VARCHAR2(20 CHAR),
    UPDATE_TM    VARCHAR2(20 CHAR),
    VER_NO       NUMBER(19)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PES_DURABLE_MAS PRIMARY KEY (DURABLE_ID)
);

CREATE TABLE PES_DURABLE_HIS (
    DURABLE_ID  VARCHAR2(40 CHAR)  NOT NULL,
    TIMEKEY     VARCHAR2(24 CHAR)  NOT NULL,
    METHOD      VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_CD    VARCHAR2(30 CHAR)  NOT NULL,
    EVENT_TM    VARCHAR2(20 CHAR),
    EVENT_DESC  VARCHAR2(200 CHAR),
    STAT_TYP    VARCHAR2(20 CHAR),
    SRC_SYSTEM  VARCHAR2(20 CHAR)  NOT NULL,
    USER_ID     VARCHAR2(30 CHAR),
    CORR_ID     VARCHAR2(60 CHAR),
    REQUEST_TM  VARCHAR2(20 CHAR),
    LOCALE      VARCHAR2(20 CHAR),
    CONSTRAINT PK_PES_DURABLE_HIS PRIMARY KEY (DURABLE_ID, TIMEKEY)
);

-- ---------------------------------------------------------------------
-- IDEMPOTENCY (correlationId 기반 중복 처리 방지)
-- ---------------------------------------------------------------------
CREATE TABLE PES_PROCESSED_MSG (
    CORR_ID      VARCHAR2(60 CHAR)  NOT NULL,
    ENTITY_TYPE  VARCHAR2(20 CHAR),
    ENTITY_ID    VARCHAR2(40 CHAR),
    STAT_TYP     VARCHAR2(20 CHAR),
    RESULT_JSON  CLOB,
    PROCESSED_TM VARCHAR2(20 CHAR),
    CONSTRAINT PK_PES_PROCESSED_MSG PRIMARY KEY (CORR_ID)
);

-- 도메인 간 연계 조회용 보조 인덱스(LOT 기준 WF/DURABLE 탐색)
CREATE INDEX IX_PES_WF_MAS_LOT_ID      ON PES_WF_MAS (LOT_ID);
CREATE INDEX IX_PES_DURABLE_MAS_LOT_ID ON PES_DURABLE_MAS (LOT_ID);
