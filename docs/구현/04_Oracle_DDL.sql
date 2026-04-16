-- ============================================================================
-- Short URL Service - Oracle DDL Script
-- ============================================================================
-- 작성일: 2026-02-09
-- 설명: 단축 URL 서비스를 위한 오라클 테이블 및 인덱스 생성 스크립트
-- ============================================================================

-- ============================================================================
-- 1. 공용 시퀀스 생성 (모든 테이블에서 공유)
-- ============================================================================
CREATE SEQUENCE SEQ_SHORT_URL_COMMON
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

COMMENT ON SEQUENCE SEQ_COMMON IS 'SHORT URL 공용 시퀀스';

-- ============================================================================
-- 2. 테이블 생성
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1. 사용자 테이블 (TBL_USER)
-- ----------------------------------------------------------------------------
CREATE TABLE TBL_USER (
    ID              NUMBER(19)          NOT NULL,
    USERNAME        VARCHAR2(100)       NOT NULL,
    GROUP_NAME      VARCHAR2(200),
    API_KEY         VARCHAR2(500),
    REFRESH_TOKEN   VARCHAR2(500),
    IS_DEL          CHAR(1)             DEFAULT 'N' NOT NULL,
    DELETED_AT      TIMESTAMP,
    CREATED_AT      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_USER PRIMARY KEY (ID),
    CONSTRAINT UK_USER_USERNAME_APIKEY UNIQUE (USERNAME, API_KEY),
    CONSTRAINT CK_USER_IS_DEL CHECK (IS_DEL IN ('Y', 'N'))
);

-- 테이블 코멘트
COMMENT ON TABLE TBL_USER IS '사용자 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN TBL_USER.ID IS '고유 번호';
COMMENT ON COLUMN TBL_USER.USERNAME IS '사용자명';
COMMENT ON COLUMN TBL_USER.GROUP_NAME IS '고객사명';
COMMENT ON COLUMN TBL_USER.API_KEY IS 'API 인증 키';
COMMENT ON COLUMN TBL_USER.REFRESH_TOKEN IS '인증 갱신용 토큰';
COMMENT ON COLUMN TBL_USER.IS_DEL IS '삭제 여부 (Y/N)';
COMMENT ON COLUMN TBL_USER.DELETED_AT IS '삭제 일시';
COMMENT ON COLUMN TBL_USER.CREATED_AT IS '생성 일시';
COMMENT ON COLUMN TBL_USER.UPDATED_AT IS '수정 일시';

-- ----------------------------------------------------------------------------
-- 2.2. 클라이언트 액세스 키 테이블 (TBL_CLIENT_ACCESS_KEY)
-- ----------------------------------------------------------------------------
CREATE TABLE TBL_CLIENT_ACCESS_KEY (
    ID              NUMBER(19)          NOT NULL,
    NAME            VARCHAR2(100)       NOT NULL,
    KEY_VALUE       VARCHAR2(500)       NOT NULL,
    ISSUED_BY       VARCHAR2(100),
    DESCRIPTION     VARCHAR2(500),
    EXPIRES_AT      TIMESTAMP,
    LAST_USED_AT    TIMESTAMP,
    IS_ACTIVE       CHAR(1)             DEFAULT 'Y' NOT NULL,
    IS_DEL          CHAR(1)             DEFAULT 'N' NOT NULL,
    DELETED_AT      TIMESTAMP,
    CREATED_AT      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_CLIENT_ACCESS_KEY PRIMARY KEY (ID),
    CONSTRAINT UK_CLIENT_ACCESS_KEY_VALUE UNIQUE (KEY_VALUE),
    CONSTRAINT CK_CLIENT_ACCESS_KEY_IS_ACTIVE CHECK (IS_ACTIVE IN ('Y', 'N')),
    CONSTRAINT CK_CLIENT_ACCESS_KEY_IS_DEL CHECK (IS_DEL IN ('Y', 'N'))
);

-- 테이블 코멘트
COMMENT ON TABLE TBL_CLIENT_ACCESS_KEY IS '클라이언트 액세스 키 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.ID IS '고유 번호';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.NAME IS '클라이언트명';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.KEY_VALUE IS '클라이언트 키 값';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.ISSUED_BY IS '발급자';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.DESCRIPTION IS '설명 (비고)';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.EXPIRES_AT IS '만료 일시';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.LAST_USED_AT IS '최근 사용 일시';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.IS_ACTIVE IS '활성 상태 (Y/N)';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.IS_DEL IS '삭제 여부 (Y/N)';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.DELETED_AT IS '삭제 일시';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.CREATED_AT IS '생성 일시';
COMMENT ON COLUMN TBL_CLIENT_ACCESS_KEY.UPDATED_AT IS '수정 일시';

-- ----------------------------------------------------------------------------
-- 2.3. 단축 URL 테이블 (TBL_SHORT_URL)
-- ----------------------------------------------------------------------------
CREATE TABLE TBL_SHORT_URL (
    ID                      NUMBER(19)          NOT NULL,
    LONG_URL                VARCHAR2(2000)      NOT NULL,
    SHORT_URL               VARCHAR2(100)       NOT NULL,
    USER_ID                 NUMBER(19)          NOT NULL,
    CLIENT_ACCESS_KEY_ID    NUMBER(19),
    EXPIRED_AT              TIMESTAMP,
    IS_DEL                  CHAR(1)             DEFAULT 'N' NOT NULL,
    DELETED_AT              TIMESTAMP,
    BOT_TYPE                VARCHAR2(20),
    BOT_SERVICE_KEY         VARCHAR2(100),
    SURVEY_ID               VARCHAR2(50),
    SURVEY_VER              VARCHAR2(20),
    CREATED_AT              TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT              TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_SHORT_URL PRIMARY KEY (ID),
    CONSTRAINT UK_SHORT_URL_SHORT_URL UNIQUE (SHORT_URL),
    CONSTRAINT FK_SHORT_URL_USER FOREIGN KEY (USER_ID) REFERENCES TBL_USER(ID),
    CONSTRAINT FK_SHORT_URL_CLIENT_KEY FOREIGN KEY (CLIENT_ACCESS_KEY_ID) REFERENCES TBL_CLIENT_ACCESS_KEY(ID),
    CONSTRAINT CK_SHORT_URL_IS_DEL CHECK (IS_DEL IN ('Y', 'N')),
    CONSTRAINT CK_SHORT_URL_BOT_TYPE CHECK (BOT_TYPE IN ('CALLBOT', 'CHATBOT'))
);

-- 테이블 코멘트
COMMENT ON TABLE TBL_SHORT_URL IS '단축 URL 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN TBL_SHORT_URL.ID IS '고유 번호';
COMMENT ON COLUMN TBL_SHORT_URL.LONG_URL IS '원본 URL';
COMMENT ON COLUMN TBL_SHORT_URL.SHORT_URL IS '단축 키';
COMMENT ON COLUMN TBL_SHORT_URL.USER_ID IS '생성 관리자';
COMMENT ON COLUMN TBL_SHORT_URL.CLIENT_ACCESS_KEY_ID IS '클라이언트 키 (비회원 발급용)';
COMMENT ON COLUMN TBL_SHORT_URL.EXPIRED_AT IS '만료 일시';
COMMENT ON COLUMN TBL_SHORT_URL.IS_DEL IS '삭제 여부 (Y/N)';
COMMENT ON COLUMN TBL_SHORT_URL.DELETED_AT IS '삭제 일시';
COMMENT ON COLUMN TBL_SHORT_URL.BOT_TYPE IS '봇 구분 (CALLBOT, CHATBOT)';
COMMENT ON COLUMN TBL_SHORT_URL.BOT_SERVICE_KEY IS '봇 서비스 식별 키 (전화번호/세션키)';
COMMENT ON COLUMN TBL_SHORT_URL.SURVEY_ID IS '설문 식별 ID';
COMMENT ON COLUMN TBL_SHORT_URL.SURVEY_VER IS '설문 버전';
COMMENT ON COLUMN TBL_SHORT_URL.CREATED_AT IS '생성 일시';
COMMENT ON COLUMN TBL_SHORT_URL.UPDATED_AT IS '수정 일시';

-- ----------------------------------------------------------------------------
-- 2.4. 리다이렉션 히스토리 테이블 (TBL_REDIRECTION_HISTORY)
-- ----------------------------------------------------------------------------
CREATE TABLE TBL_REDIRECTION_HISTORY (
    ID                  NUMBER(19)          NOT NULL,
    SHORT_URL_ID        NUMBER(19)          NOT NULL,
    REFERER             VARCHAR2(500),
    USER_AGENT          VARCHAR2(500),
    IP                  VARCHAR2(50),
    DEVICE_TYPE         VARCHAR2(20),
    OS                  VARCHAR2(50),
    BROWSER             VARCHAR2(50),
    COUNTRY             VARCHAR2(10),
    CITY                VARCHAR2(100),
    REDIRECT_AT         TIMESTAMP           NOT NULL,
    BOT_TYPE            VARCHAR2(20),
    BOT_SERVICE_KEY     VARCHAR2(100),
    SURVEY_ID           VARCHAR2(50),
    SURVEY_VER          VARCHAR2(20),
    CONSTRAINT PK_REDIRECTION_HISTORY PRIMARY KEY (ID),
    CONSTRAINT FK_REDIRECTION_HISTORY_SHORT_URL FOREIGN KEY (SHORT_URL_ID) REFERENCES TBL_SHORT_URL(ID),
    CONSTRAINT CK_REDIRECTION_HISTORY_BOT_TYPE CHECK (BOT_TYPE IN ('CALLBOT', 'CHATBOT'))
);

-- 테이블 코멘트
COMMENT ON TABLE TBL_REDIRECTION_HISTORY IS '리다이렉션 히스토리 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.ID IS '고유 번호';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.SHORT_URL_ID IS '단축 URL 정보';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.REFERER IS '레퍼러 (이전 페이지 주소)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.USER_AGENT IS 'User Agent (브라우저 정보)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.IP IS '접속 IP 주소';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.DEVICE_TYPE IS '디바이스 구분 (Mobile, Desktop 등)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.OS IS '운영체제(OS)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.BROWSER IS '브라우저 명';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.COUNTRY IS '접속 국가 코드';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.CITY IS '접속 도시 명';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.REDIRECT_AT IS '리다이렉션 실행 일시';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.BOT_TYPE IS '봇 구분 (CALLBOT, CHATBOT)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.BOT_SERVICE_KEY IS '봇 서비스 식별 키 (참조 키)';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.SURVEY_ID IS '설문 식별 ID';
COMMENT ON COLUMN TBL_REDIRECTION_HISTORY.SURVEY_VER IS '설문 버전';

-- ============================================================================
-- 3. 인덱스 생성
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1. TBL_USER 인덱스
-- ----------------------------------------------------------------------------
-- USERNAME으로 조회 (로그인, 토큰 발급)
CREATE INDEX IDX_USER_USERNAME ON TBL_USER(USERNAME);

-- USERNAME + REFRESH_TOKEN으로 조회 (토큰 재발급)
CREATE INDEX IDX_USER_USERNAME_REFRESH ON TBL_USER(USERNAME, REFRESH_TOKEN);

-- 삭제되지 않은 사용자 조회
CREATE INDEX IDX_USER_IS_DEL_CREATED ON TBL_USER(IS_DEL, CREATED_AT DESC);

-- ----------------------------------------------------------------------------
-- 3.2. TBL_CLIENT_ACCESS_KEY 인덱스
-- ----------------------------------------------------------------------------
-- KEY_VALUE + IS_DEL로 조회 (키 검증)
CREATE INDEX IDX_CLIENT_KEY_VALUE_DEL ON TBL_CLIENT_ACCESS_KEY(KEY_VALUE, IS_DEL);

-- IS_ACTIVE + IS_DEL + CREATED_AT으로 조회 (활성 키 목록)
CREATE INDEX IDX_CLIENT_KEY_ACTIVE ON TBL_CLIENT_ACCESS_KEY(IS_ACTIVE, IS_DEL, CREATED_AT DESC);

-- ----------------------------------------------------------------------------
-- 3.3. TBL_SHORT_URL 인덱스
-- ----------------------------------------------------------------------------
-- SHORT_URL로 조회 (리다이렉션) - UNIQUE 제약조건이 자동 인덱스 생성하므로 생략

-- USER_ID로 조회 (사용자별 단축 URL 목록)
CREATE INDEX IDX_SHORT_URL_USER_ID ON TBL_SHORT_URL(USER_ID, IS_DEL, CREATED_AT DESC);

-- CLIENT_ACCESS_KEY_ID로 조회
CREATE INDEX IDX_SHORT_URL_CLIENT_KEY_ID ON TBL_SHORT_URL(CLIENT_ACCESS_KEY_ID, IS_DEL);

-- 삭제되지 않은 URL 페이징 조회
CREATE INDEX IDX_SHORT_URL_IS_DEL_CREATED ON TBL_SHORT_URL(IS_DEL, CREATED_AT DESC);

-- BOT_TYPE + BOT_SERVICE_KEY로 조회
CREATE INDEX IDX_SHORT_URL_BOT_INFO ON TBL_SHORT_URL(BOT_TYPE, BOT_SERVICE_KEY, IS_DEL);

-- SURVEY_ID + SURVEY_VER로 조회
CREATE INDEX IDX_SHORT_URL_SURVEY_INFO ON TBL_SHORT_URL(SURVEY_ID, SURVEY_VER, IS_DEL);

-- ----------------------------------------------------------------------------
-- 3.4. TBL_REDIRECTION_HISTORY 인덱스
-- ----------------------------------------------------------------------------
-- SHORT_URL_ID로 카운트 조회
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_ID ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID);

-- SHORT_URL_ID + REDIRECT_AT으로 조회 (시간대별 통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_DATE ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, REDIRECT_AT);

-- SHORT_URL_ID + REFERER로 그룹핑 (통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_REFERER ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, REFERER);

-- SHORT_URL_ID + DEVICE_TYPE로 그룹핑 (통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_DEVICE ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, DEVICE_TYPE);

-- SHORT_URL_ID + OS로 그룹핑 (통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_OS ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, OS);

-- SHORT_URL_ID + BROWSER로 그룹핑 (통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_BROWSER ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, BROWSER);

-- SHORT_URL_ID + COUNTRY로 그룹핑 (통계)
CREATE INDEX IDX_REDIR_HIST_SHORT_URL_COUNTRY ON TBL_REDIRECTION_HISTORY(SHORT_URL_ID, COUNTRY);

-- BOT_TYPE + BOT_SERVICE_KEY로 조회
CREATE INDEX IDX_REDIR_HIST_BOT_INFO ON TBL_REDIRECTION_HISTORY(BOT_TYPE, BOT_SERVICE_KEY);

-- ============================================================================
-- 4. 트리거 생성 (자동 증가 ID 및 수정 일시 업데이트)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1. TBL_USER 트리거
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_USER_BI
BEFORE INSERT ON TBL_USER
FOR EACH ROW
BEGIN
    IF :NEW.ID IS NULL THEN
        SELECT SEQ_COMMON.NEXTVAL INTO :NEW.ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_USER_BU
BEFORE UPDATE ON TBL_USER
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- ----------------------------------------------------------------------------
-- 4.2. TBL_CLIENT_ACCESS_KEY 트리거
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CLIENT_KEY_BI
BEFORE INSERT ON TBL_CLIENT_ACCESS_KEY
FOR EACH ROW
BEGIN
    IF :NEW.ID IS NULL THEN
        SELECT SEQ_COMMON.NEXTVAL INTO :NEW.ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_CLIENT_KEY_BU
BEFORE UPDATE ON TBL_CLIENT_ACCESS_KEY
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- ----------------------------------------------------------------------------
-- 4.3. TBL_SHORT_URL 트리거
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_SHORT_URL_BI
BEFORE INSERT ON TBL_SHORT_URL
FOR EACH ROW
BEGIN
    IF :NEW.ID IS NULL THEN
        SELECT SEQ_COMMON.NEXTVAL INTO :NEW.ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_SHORT_URL_BU
BEFORE UPDATE ON TBL_SHORT_URL
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- ----------------------------------------------------------------------------
-- 4.4. TBL_REDIRECTION_HISTORY 트리거
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_REDIR_HIST_BI
BEFORE INSERT ON TBL_REDIRECTION_HISTORY
FOR EACH ROW
BEGIN
    IF :NEW.ID IS NULL THEN
        SELECT SEQ_COMMON.NEXTVAL INTO :NEW.ID FROM DUAL;
    END IF;
    IF :NEW.REDIRECT_AT IS NULL THEN
        :NEW.REDIRECT_AT := SYSTIMESTAMP;
    END IF;
END;
/

-- ============================================================================
-- 5. 테이블 통계 수집 (옵티마이저 성능 향상)
-- ============================================================================
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'TBL_USER');
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'TBL_CLIENT_ACCESS_KEY');
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'TBL_SHORT_URL');
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'TBL_REDIRECTION_HISTORY');

-- ============================================================================
-- 6. 권한 부여 (필요한 경우)
-- ============================================================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TBL_USER TO [사용자명];
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TBL_CLIENT_ACCESS_KEY TO [사용자명];
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TBL_SHORT_URL TO [사용자명];
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TBL_REDIRECTION_HISTORY TO [사용자명];
-- GRANT SELECT ON SEQ_COMMON TO [사용자명];

-- ============================================================================
-- END OF SCRIPT
-- ============================================================================
