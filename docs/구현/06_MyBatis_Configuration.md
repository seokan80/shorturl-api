# MyBatis 설정 가이드

## 1. 의존성 추가 (build.gradle)

```gradle
dependencies {
    // MyBatis Spring Boot Starter
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'

    // Oracle JDBC Driver
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'

    // 기타 기존 의존성...
}
```

## 2. application.yml 설정

```yaml
spring:
  datasource:
    driver-class-name: oracle.jdbc.OracleDriver
    url: jdbc:oracle:thin:@localhost:1521:ORCL
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# MyBatis 설정
mybatis:
  # Mapper XML 파일 위치
  mapper-locations: classpath:mapper/**/*.xml
  # 타입 별칭 패키지
  type-aliases-package: com.nh.shorturl.admin.entity
  # 설정 파일 위치
  config-location: classpath:mybatis-config.xml
  configuration:
    # 카멜케이스 자동 변환 (username -> USERNAME)
    map-underscore-to-camel-case: true
    # 결과가 null인 경우도 매핑
    call-setters-on-nulls: true
    # JDBC 타입 자동 추론
    jdbc-type-for-null: NULL
    # 로그 설정
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.nh.shorturl.admin.repository: DEBUG
```

## 3. mybatis-config.xml 설정

`src/main/resources/mybatis-config.xml` 파일 생성:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <!-- 카멜케이스 자동 변환 -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!-- 지연 로딩 활성화 -->
        <setting name="lazyLoadingEnabled" value="true"/>
        <!-- 적극적인 지연 로딩 비활성화 -->
        <setting name="aggressiveLazyLoading" value="false"/>
        <!-- 여러 ResultSet 허용 -->
        <setting name="multipleResultSetsEnabled" value="true"/>
        <!-- 컬럼명 대신 라벨 사용 -->
        <setting name="useColumnLabel" value="true"/>
        <!-- 생성된 키 사용 -->
        <setting name="useGeneratedKeys" value="true"/>
        <!-- 자동 매핑 동작 -->
        <setting name="autoMappingBehavior" value="PARTIAL"/>
        <!-- 자동 매핑 알 수 없는 컬럼 동작 -->
        <setting name="autoMappingUnknownColumnBehavior" value="WARNING"/>
        <!-- 기본 Executor 타입 -->
        <setting name="defaultExecutorType" value="SIMPLE"/>
        <!-- 타임아웃 설정 (초) -->
        <setting name="defaultStatementTimeout" value="25"/>
        <!-- 페치 사이즈 -->
        <setting name="defaultFetchSize" value="100"/>
        <!-- 배치 업데이트 사용 -->
        <setting name="defaultResultSetType" value="FORWARD_ONLY"/>
        <!-- NULL 허용 -->
        <setting name="callSettersOnNulls" value="true"/>
        <!-- 로그 접두사 -->
        <setting name="logPrefix" value="[MyBatis] "/>
        <!-- 로그 구현체 -->
        <setting name="logImpl" value="SLF4J"/>
    </settings>

    <typeHandlers>
        <!-- Y/N을 Boolean으로 변환하는 커스텀 TypeHandler -->
        <typeHandler handler="com.nh.shorturl.admin.config.YesNoTypeHandler"
                     javaType="java.lang.Boolean"/>
    </typeHandlers>
</configuration>
```

## 4. YesNoTypeHandler 구현

`src/main/java/com/nh/shorturl/admin/config/YesNoTypeHandler.java`:

```java
package com.nh.shorturl.admin.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Oracle의 Y/N 값을 Java Boolean으로 변환하는 TypeHandler
 */
@MappedTypes(Boolean.class)
public class YesNoTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter ? "Y" : "N");
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return "Y".equals(value);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return "Y".equals(value);
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return "Y".equals(value);
    }
}
```

## 5. Repository 인터페이스 변경

Spring Data JPA에서 MyBatis로 전환하려면 Repository 인터페이스를 수정해야 합니다.

### 기존 (Spring Data JPA)
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### 변경 후 (MyBatis)
```java
@Mapper
public interface UserRepository {
    User findByUsername(String username);
    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();
    void insert(User user);
    void update(User user);
    void delete(Long id);
}
```

## 6. 디렉토리 구조

```
short-url-admin/
└── src/main/
    ├── java/com/nh/shorturl/admin/
    │   ├── config/
    │   │   └── YesNoTypeHandler.java
    │   └── repository/
    │       ├── UserRepository.java (MyBatis Mapper)
    │       ├── ClientAccessKeyRepository.java
    │       ├── ShortUrlRepository.java
    │       └── RedirectionHistoryRepository.java
    └── resources/
        ├── mybatis-config.xml
        ├── mapper/
        │   ├── UserMapper.xml
        │   ├── ClientAccessKeyMapper.xml
        │   ├── ShortUrlMapper.xml
        │   └── RedirectionHistoryMapper.xml
        └── application.yml
```

## 7. Service 계층 수정 예시

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createUser(String username) {
        User user = User.builder()
            .username(username)
            .build();

        // MyBatis는 insert 후 자동으로 ID를 설정해줌 (selectKey 사용)
        userRepository.insert(user);
        return user;
    }

    @Override
    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + username);
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        userRepository.update(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.delete(id);
    }
}
```

## 8. 페이징 처리

MyBatis에서는 Oracle의 `OFFSET ... FETCH NEXT` 구문을 사용합니다.

```java
// PageRequest를 offset, limit으로 변환
@Service
public class ShortUrlService {

    public Page<ShortUrl> findAll(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<ShortUrl> content = shortUrlRepository.findAll(offset, limit);
        long total = shortUrlRepository.count();

        return new PageImpl<>(content, pageable, total);
    }
}
```

## 9. 트랜잭션 관리

MyBatis도 Spring의 `@Transactional` 어노테이션을 그대로 사용할 수 있습니다.

```java
@Service
@Transactional
public class ShortUrlServiceImpl implements ShortUrlService {

    @Override
    public ShortUrl createShortUrl(ShortUrlRequest request) {
        // 여러 쿼리가 하나의 트랜잭션으로 실행됨
        User user = userRepository.findByUsername(request.getUsername());

        ShortUrl shortUrl = ShortUrl.builder()
            .longUrl(request.getLongUrl())
            .shortUrl(generateShortUrl())
            .build();

        shortUrlRepository.insert(shortUrl);

        return shortUrl;
    }
}
```

## 10. 로깅 설정

`logback-spring.xml`에 MyBatis 로깅 추가:

```xml
<logger name="com.nh.shorturl.admin.repository" level="DEBUG"/>
<logger name="org.mybatis" level="DEBUG"/>
<logger name="java.sql.Connection" level="DEBUG"/>
<logger name="java.sql.Statement" level="DEBUG"/>
<logger name="java.sql.PreparedStatement" level="DEBUG"/>
<logger name="java.sql.ResultSet" level="DEBUG"/>
```

## 11. 테스트 작성

```java
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testInsertAndFind() {
        // given
        User user = User.builder()
            .username("testuser")
            .groupName("testgroup")
            .build();

        // when
        userRepository.insert(user);
        User found = userRepository.findByUsername("testuser");

        // then
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals("testuser", found.getUsername());
    }
}
```

## 12. JPA에서 MyBatis로 전환 체크리스트

- [ ] build.gradle에 mybatis-spring-boot-starter 추가
- [ ] application.yml에 MyBatis 설정 추가
- [ ] mybatis-config.xml 생성
- [ ] YesNoTypeHandler 구현
- [ ] Mapper XML 파일들을 resources/mapper/ 하위에 배치
- [ ] Repository 인터페이스에서 `extends JpaRepository` 제거 후 `@Mapper` 추가
- [ ] Service 계층에서 Repository 메서드 호출 방식 수정
- [ ] Entity 클래스에서 JPA 어노테이션 제거 (또는 유지 - MyBatis는 POJO 사용)
- [ ] 페이징 처리 로직 수정
- [ ] 테스트 코드 수정
- [ ] 로깅 설정 확인

## 13. 성능 최적화 팁

### 13.1. Batch Insert 사용
```xml
<insert id="batchInsert" parameterType="list">
    INSERT ALL
    <foreach collection="list" item="item" separator=" ">
        INTO TBL_SHORT_URL (ID, LONG_URL, SHORT_URL, USER_ID, CREATED_AT, UPDATED_AT)
        VALUES (SEQ_COMMON.NEXTVAL, #{item.longUrl}, #{item.shortUrl}, #{item.userId}, SYSTIMESTAMP, SYSTIMESTAMP)
    </foreach>
    SELECT * FROM DUAL
</insert>
```

### 13.2. ResultMap 재사용
```xml
<!-- 기본 ResultMap -->
<resultMap id="baseResultMap" type="User">
    <id property="id" column="ID"/>
    <result property="username" column="USERNAME"/>
</resultMap>

<!-- 확장 ResultMap -->
<resultMap id="extendedResultMap" type="User" extends="baseResultMap">
    <result property="groupName" column="GROUP_NAME"/>
</resultMap>
```

### 13.3. 2차 캐시 활성화
```xml
<mapper namespace="com.nh.shorturl.admin.repository.UserRepository">
    <!-- 읽기 전용 캐시 설정 -->
    <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>
</mapper>
```

## 14. 문제 해결

### 14.1. Mapper를 찾을 수 없는 경우
```
Error: Invalid bound statement (not found): com.nh.shorturl.admin.repository.UserRepository.findByUsername
```

**해결 방법:**
1. `application.yml`의 `mapper-locations` 경로 확인
2. Mapper XML의 `namespace`가 Repository 인터페이스 풀 패키지명과 일치하는지 확인
3. Mapper XML 파일이 빌드 결과물에 포함되었는지 확인

### 14.2. TypeHandler 적용 안 되는 경우
```java
// Mapper에서 명시적으로 지정
<result property="deleted" column="IS_DEL"
        typeHandler="com.nh.shorturl.admin.config.YesNoTypeHandler"/>
```

### 14.3. Oracle 시퀀스가 동작하지 않는 경우
```xml
<!-- order="BEFORE"로 변경 -->
<selectKey keyProperty="id" resultType="long" order="BEFORE">
    SELECT SEQ_COMMON.NEXTVAL FROM DUAL
</selectKey>
```

---

## 참고 자료

- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [Oracle SQL 레퍼런스](https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/)
