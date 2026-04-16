# Short URL Service - 구현 문서

## 📋 문서 목록

본 디렉토리에는 Short URL 서비스의 오라클 DB 구축 및 MyBatis 전환을 위한 문서들이 포함되어 있습니다.

### 1. 도메인 설계 (01_Domain_Design_Backend.md)
- 백엔드 도메인 모델 설계 문서
- 엔티티 관계도 및 비즈니스 로직

### 2. API 설계 명세 (02_API_Design_Spec.md)
- REST API 엔드포인트 명세
- 요청/응답 스펙
- 에러 코드 정의

### 3. DDL 및 인덱스 설계 (03_DDL_and_Index_Design.sql)
- 기존 DDL 파일 (참고용)

### 4. **오라클 DDL 스크립트** (04_Oracle_DDL.sql) ⭐ **NEW**
엔티티 기반으로 생성된 오라클 DDL 스크립트입니다.

#### 주요 특징:
- ✅ **공용 시퀀스 (SEQ_COMMON)**: 모든 테이블이 하나의 시퀀스 공유
- ✅ **4개 테이블 생성**:
  - `TBL_USER` - 사용자 정보
  - `TBL_CLIENT_ACCESS_KEY` - 클라이언트 액세스 키
  - `TBL_SHORT_URL` - 단축 URL 정보
  - `TBL_REDIRECTION_HISTORY` - 리다이렉션 히스토리
- ✅ **완전한 컬럼 코멘트**: 모든 테이블/컬럼에 한글 설명 추가
- ✅ **최적화된 인덱스**: 19개의 성능 최적화 인덱스
  - 검색 패턴 기반 복합 인덱스
  - 통계 쿼리 최적화 인덱스
  - 페이징 최적화 인덱스
- ✅ **자동 트리거**: ID 자동 증가 및 수정 일시 자동 업데이트
- ✅ **제약 조건**: PK, FK, UK, CHECK 제약 조건 완비

#### 실행 방법:
```sql
-- Oracle SQL*Plus 또는 SQL Developer에서 실행
@04_Oracle_DDL.sql
```

---

### 5. **MyBatis 매퍼 XML** (05_MyBatis_Mappers.xml) ⭐ **NEW**
Spring Data JPA Repository를 MyBatis 매퍼로 변환한 XML 파일입니다.

#### 포함된 매퍼:
1. **UserMapper** - 사용자 CRUD 및 조회
   - `findByUsername`: 사용자명으로 조회
   - `findByUsernameAndRefreshToken`: 토큰 재발급용 조회
   - `findAllByDeletedFalseOrderByCreatedAtDesc`: 삭제되지 않은 사용자 목록

2. **ClientAccessKeyMapper** - 클라이언트 키 관리
   - `findByKeyValueAndDeletedFalse`: 키 검증
   - `updateLastUsedAt`: 최근 사용 일시 업데이트

3. **ShortUrlMapper** - 단축 URL CRUD
   - `findByShortUrl`: 단축 URL 키로 조회 (리다이렉션용)
   - `existsByShortUrl`: 중복 체크
   - `findAll`: 페이징 조회
   - `findByUser`: 사용자별 단축 URL 조회

4. **RedirectionHistoryMapper** - 리다이렉션 통계
   - `countByShortUrlId`: 총 리다이렉션 횟수
   - `getStatsByShortUrlId`: 동적 그룹핑 통계 (REFERER, DEVICE_TYPE, OS, BROWSER, YEAR, MONTH 등)

#### 주요 특징:
- ✅ **동적 쿼리**: `<foreach>`, `<choose>` 활용한 유연한 쿼리
- ✅ **페이징 지원**: Oracle `OFFSET ... FETCH` 구문 사용
- ✅ **자동 ID 생성**: `<selectKey>`로 시퀀스 자동 할당
- ✅ **Y/N ↔ Boolean 변환**: TypeHandler 활용
- ✅ **Enum 지원**: `BotType`, `GroupingType` 등 Enum 매핑

---

### 6. **MyBatis 설정 가이드** (06_MyBatis_Configuration.md) ⭐ **NEW**
Spring Data JPA에서 MyBatis로 전환하는 전체 가이드입니다.

#### 포함 내용:
1. **의존성 추가** (build.gradle)
2. **application.yml 설정**
3. **mybatis-config.xml 설정**
4. **YesNoTypeHandler 구현** (Y/N ↔ Boolean 변환)
5. **Repository 인터페이스 변경 방법**
6. **디렉토리 구조**
7. **Service 계층 수정 예시**
8. **페이징 처리 방법**
9. **트랜잭션 관리**
10. **로깅 설정**
11. **테스트 작성**
12. **JPA → MyBatis 전환 체크리스트**
13. **성능 최적화 팁**
    - Batch Insert
    - ResultMap 재사용
    - 2차 캐시
14. **문제 해결 가이드**

---

## 🚀 빠른 시작 가이드

### Step 1: 오라클 DB 생성
```bash
# SQL*Plus 또는 SQL Developer에서 실행
sqlplus username/password@localhost:1521/ORCL
@docs/구현/04_Oracle_DDL.sql
```

### Step 2: MyBatis 의존성 추가
```gradle
// short-url-admin/build.gradle
dependencies {
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'
}
```

### Step 3: Mapper XML 배치
```bash
# Mapper XML을 적절한 위치에 배치
mkdir -p short-url-admin/src/main/resources/mapper
cp docs/구현/05_MyBatis_Mappers.xml short-url-admin/src/main/resources/mapper/
```

### Step 4: 설정 파일 작성
`06_MyBatis_Configuration.md` 문서를 참고하여:
- `application.yml` 수정
- `mybatis-config.xml` 생성
- `YesNoTypeHandler` 구현

### Step 5: Repository 인터페이스 수정
```java
// 기존
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// 변경
@Mapper
public interface UserRepository {
    User findByUsername(String username);
    void insert(User user);
}
```

### Step 6: 테스트 및 검증
```bash
./gradlew :short-url-admin:test
```

---

## 📊 테이블 구조 요약

| 테이블명 | 설명 | 주요 컬럼 | 인덱스 개수 |
|---------|------|----------|-----------|
| TBL_USER | 사용자 정보 | ID, USERNAME, API_KEY, REFRESH_TOKEN | 3개 |
| TBL_CLIENT_ACCESS_KEY | 클라이언트 키 | ID, KEY_VALUE, IS_ACTIVE, EXPIRES_AT | 2개 |
| TBL_SHORT_URL | 단축 URL | ID, LONG_URL, SHORT_URL, USER_ID | 5개 |
| TBL_REDIRECTION_HISTORY | 리다이렉션 히스토리 | ID, SHORT_URL_ID, REDIRECT_AT, REFERER | 8개 |

**총 시퀀스**: 1개 (SEQ_COMMON - 공용)
**총 인덱스**: 19개 (성능 최적화)
**총 트리거**: 8개 (ID 자동 증가 4개 + 수정 일시 업데이트 4개)

---

## 🔍 주요 쿼리 패턴

### 1. 사용자 조회 (인증)
```sql
-- USERNAME + REFRESH_TOKEN으로 조회
-- 사용 인덱스: IDX_USER_USERNAME_REFRESH
SELECT * FROM TBL_USER
WHERE USERNAME = ? AND REFRESH_TOKEN = ? AND IS_DEL = 'N';
```

### 2. 단축 URL 리다이렉션
```sql
-- SHORT_URL 키로 조회
-- 사용 인덱스: UK_SHORT_URL_SHORT_URL (자동 생성)
SELECT * FROM TBL_SHORT_URL
WHERE SHORT_URL = ? AND IS_DEL = 'N';
```

### 3. 리다이렉션 통계
```sql
-- SHORT_URL_ID + REFERER로 그룹핑
-- 사용 인덱스: IDX_REDIR_HIST_SHORT_URL_REFERER
SELECT REFERER, COUNT(*) AS count
FROM TBL_REDIRECTION_HISTORY
WHERE SHORT_URL_ID = ?
GROUP BY REFERER
ORDER BY COUNT(*) DESC;
```

### 4. 페이징 조회
```sql
-- 삭제되지 않은 단축 URL 페이징
-- 사용 인덱스: IDX_SHORT_URL_IS_DEL_CREATED
SELECT * FROM TBL_SHORT_URL
WHERE IS_DEL = 'N'
ORDER BY CREATED_AT DESC
OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
```

---

## 📈 성능 고려사항

### 인덱스 전략
1. **단일 컬럼 인덱스**: 고유 제약조건 (USERNAME, SHORT_URL, KEY_VALUE)
2. **복합 인덱스**: 검색 + 정렬 패턴 (IS_DEL + CREATED_AT)
3. **통계 최적화 인덱스**: 그룹핑 쿼리용 (SHORT_URL_ID + REFERER/DEVICE_TYPE/OS 등)

### 시퀀스 공유의 장점
- ✅ 관리 포인트 단순화
- ✅ 분산 환경에서 전역 고유성 보장
- ✅ 테이블 간 순서 추적 가능

### 주의사항
- ⚠️ `TBL_REDIRECTION_HISTORY`는 데이터 증가 속도가 빠름 → 파티셔닝 고려
- ⚠️ 통계 쿼리는 집계 함수 사용 → 적절한 인덱스 필수
- ⚠️ 논리 삭제(Soft Delete) 사용 → `IS_DEL` 필터 필수

---

## 🛠️ 유지보수

### 정기 작업
```sql
-- 통계 정보 갱신 (월 1회 권장)
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'TBL_REDIRECTION_HISTORY');

-- 인덱스 재구성 (분기 1회 권장)
ALTER INDEX IDX_REDIR_HIST_SHORT_URL_ID REBUILD;
```

### 모니터링 쿼리
```sql
-- 테이블 크기 확인
SELECT TABLE_NAME, NUM_ROWS, BLOCKS
FROM USER_TABLES
WHERE TABLE_NAME LIKE 'TBL_%'
ORDER BY NUM_ROWS DESC;

-- 인덱스 효율 확인
SELECT INDEX_NAME, CLUSTERING_FACTOR, NUM_ROWS
FROM USER_INDEXES
WHERE TABLE_NAME = 'TBL_REDIRECTION_HISTORY';
```

---

## 📞 문의 및 지원

- **작성자**: Claude Code
- **작성일**: 2026-02-09
- **프로젝트**: Short URL Service
- **버전**: 0.0.1-SNAPSHOT

---

## 📚 추가 참고 자료

- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/ko/index.html)
- [Oracle SQL 레퍼런스](https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/)
- [Spring Boot MyBatis Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
