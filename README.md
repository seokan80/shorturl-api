# Short URL Service

## 1. 프로젝트 소개

원본 URL을 입력받아 짧은 URL을 생성해주고, 단축된 URL로 접근 시 원본 URL로 리디렉션해주는 서비스입니다.

주요 기능:
- 사용자별 API Key 발급
- 단축 URL 생성
- 단축 URL 리디렉션
- 리디렉션 통계 조회

## 2. 기술 스택

- Java 17
- Spring Boot 3.5.7 (Gradle 8.14.3)
- Spring Data JPA
- Spring MVC
- QueryDSL
- Oracle
- H2 Database (Only Running in Local)
- Lombok
- JWT

## 3. 빌드 및 실행 방법

이 프로젝트는 `local`, `dev`, `prod` 세 가지 환경 프로파일을 지원합니다. 빌드 시 프로파일을 지정하여 각 환경에 맞는 JAR 파일을 생성할 수 있습니다.

### 3.1. 빌드 스크립트 사용 (권장)

프로젝트 루트의 `build.sh` 스크립트를 사용하면 간편하게 환경별 빌드를 수행할 수 있습니다.

```bash
# 스크립트 실행 권한 부여 (최초 1회)
chmod +x build.sh
```

```bash
# Local 환경 빌드 (기본)
./build.sh local
```

```bash
# Development 환경 빌드
./build.sh dev
```

```bash
# Production 환경 빌드
./build.sh prod
```
### 3.2 Gradle 직접 사용

Gradle 명령어를 직접 사용하여 빌드할 수도 있습니다. -Pprofile 파라미터로 프로파일을 지정합니다.
```bash
# Local 환경 빌드 (기본값)
./gradlew build -Pprofile=local
```
```bash
# Development 환경 빌드
./gradlew build -Pprofile=dev
```
```bash
# Production 환경 빌드
./gradlew build -Pprofile=prod
```

### 3.3 애플리케이션 실행

빌드된 JAR 파일을 실행하여 애플리케이션을 시작합니다. Jasypt 암호화 키를 환경변수로 설정한 후 실행해야 합니다.
```bash
# (예시) Jasypt 마스터 키 환경변수 설정
export JASYPT_ENCRYPTOR_PASSWORD=your_master_key

# (예시) Production 빌드 결과물 실행
java -jar build/libs/short-url-prod-0.0.1-SNAPSHOT.jar
```

## 4. API 테스트

프로젝트 루트에 포함된 `api-flow.http` 파일과 `url_shortener_script.sh` 스크립트를 사용하여 주요 API 흐름을 테스트할 수 있습니다.

### `api-flow.http` 사용법

- IntelliJ IDEA Ultimate 버전에서 `api-flow.http` 파일을 열고 각 요청의 왼쪽에 있는 실행 버튼(▶)을 클릭하여 API를 테스트할 수 있습니다.
- 변수는 파일 상단에 정의되어 있으며, 필요에 따라 수정하여 사용할 수 있습니다.

### `url_shortener_script.sh` 사용법

- 터미널에서 스크립트를 직접 실행하여 API 흐름을 테스트합니다.
- 스크립트 실행을 위해서는 `jq`가 시스템에 설치되어 있어야 합니다.
  ```bash
  # 스크립트 실행 권한 부여
  chmod +x url_shortener_script.sh

  # 스크립트 실행
  ./url_shortener_script.sh
  ```

## 5. API 엔드포인트 상세

모든 API 응답은 아래와 같은 공통된 구조를 가집니다.

**공통 응답 구조 (Common Response)**
```json
{
  "status": "OK",
  "message": "SUCCESS",
  "data": {
    // API별 개별 데이터가 이 안에 위치합니다.
  }
}
```
*   `status`: HTTP 상태와 유사한 응답 상태 코드 (예: `OK`, `BAD_REQUEST`)
*   `message`: 응답 결과에 대한 메시지
*   `data`: 실제 비즈니스 데이터가 담기는 객체

---

### 5.1. 인증

#### `POST /api/auth/register`

- **설명**: 새로운 사용자를 등록합니다. 토큰 발급은 별도의 `/token/issue` 엔드포인트에서 수행됩니다.
- **Header**: `X-REGISTRATION-KEY: {{registration_key}}`
- **Body**:
  ```json
  {
    "username": "my-awesome-service"
  }
  ```
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "id": 42,
    "username": "my-awesome-service",
    "createdAt": "2025-01-13T12:00:00",
    "updatedAt": "2025-01-13T12:00:00"
  }
  ```

#### `POST /api/auth/token/issue`

- **설명**: 등록된 사용자 계정으로 Access/Refresh Token을 발급합니다.
- **Header**: `X-REGISTRATION-KEY: {{registration_key}}`
- **Body**:
  ```json
  {
    "username": "my-awesome-service"
  }
  ```
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "token": "{발급된_API_Key}",
    "refreshToken": "{발급된_Refresh_Token}"
  }
  ```

#### `POST /api/auth/token/re-issue`

- **설명**: Refresh Token을 검증하고 Access/Refresh Token을 재발급합니다.
- **Header**: `X-REGISTRATION-KEY: {{registration_key}}`
- **Body**:
  ```json
  {
    "username": "my-awesome-service",
    "refreshToken": "{저장된_Refresh_Token}"
  }
  ```
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "token": "{재발급된_API_Key}",
    "refreshToken": "{재발급된_Refresh_Token}"
  }
  ```

### 5.2. 단축 URL

- **설명**: Refresh Token을 검증하고 Access/Refresh Token을 재발급합니다.
- **Header**: `X-REGISTRATION-KEY: {{registration_key}}`
- **Body**:
  ```json
  {
    "username": "my-awesome-service",
    "refreshToken": "{저장된_Refresh_Token}"
  }
  ```
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "token": "{재발급된_API_Key}",
    "refreshToken": "{재발급된_Refresh_Token}"
  }
  ```

#### `GET /api/auth/users/{username}`

- **설명**: 특정 사용자의 기본 정보를 조회합니다. API Key, Refresh Token 등 민감 정보는 포함하지 않습니다.
- **Header**: `X-REGISTRATION-KEY: {{registration_key}}`
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "id": 1,
    "username": "my-awesome-service",
    "createdAt": "2025-01-13T12:00:00",
    "updatedAt": "2025-01-13T12:00:00"
  }
  ```

#### `POST /api/short-url`

- **설명**: 원본 URL을 전달하여 새로운 단축 URL을 생성합니다.
- **Header**: `Authorization: Bearer {{token}}`
- **Body**:
  ```json
  {
    "longUrl": "https://www.google.com"
  }
  ```
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "id": 1,
    "longUrl": "https://www.google.com",
    "shortUrl": "http://localhost:8080/r/B",
    "username": "my-awesome-service",
    "createdAt": "2025-10-22T12:34:56.789"
  }
  ```

#### `GET /api/short-url/{id}`

- **설명**: 단축 URL의 ID를 사용하여 상세 정보를 조회합니다.
- **Header**: `Authorization: Bearer {{token}}`
- **Path Variable**: `id` - 조회할 단축 URL의 ID
- **성공 응답**: `POST /api/short-url` 성공 응답과 동일합니다.

#### `GET /api/short-url/key/{shortUrlKey}`

- **설명**: 단축 URL의 키(예: `B`)를 사용하여 상세 정보를 조회합니다.
- **Header**: `Authorization: Bearer {{token}}`
- **Path Variable**: `shortUrlKey` - 조회할 단축 URL의 키
- **성공 응답**: `POST /api/short-url` 성공 응답과 동일합니다.

#### `POST /api/short-url/delete/{id}`

- **설명**: 단축 URL의 ID를 사용하여 단축 URL 정보를 삭제합니다.
- **Header**: `Authorization: Bearer {{token}}`
- **Path Variable**: `id` - 삭제할 단축 URL의 ID
- **성공 응답 (`data` 필드 내부)**: `true`

#### `GET /r/{shortUrlKey}`

- **설명**: 생성된 단축 URL 키로 접근 시 원본 URL로 리디렉션합니다. 이 과정에서 리디렉션 통계가 기록됩니다.

### 5.3. 리디렉션 통계

#### `GET /r/history/{shortUrlId}/count`

- **설명**: 특정 단축 URL이 리디렉션된 총 횟수를 조회합니다.
- **Path Variable**: `shortUrlId` - 단축 URL 생성 시 응답받은 `id` 값
- **성공 응답 (`data` 필드 내부)**:
  ```json
  {
    "count": 10
  }
  ```

#### `POST /r/history/{shortUrlId}/stats`

- **설명**: 리디렉션 기록을 다양한 조건으로 그룹화하여 통계를 조회합니다.
- **Path Variable**: `shortUrlId` - 단축 URL 생성 시 응답받은 `id` 값
- **Body**:
  ```json
  {
    "groupBy": ["REFERER", "YEAR"]
  }
  ```
- **`groupBy` 사용 가능 값**: `REFERER`, `USER_AGENT`, `YEAR`, `MONTH`, `DAY`, `HOUR`
- **성공 응답 (`data` 필드 내부)**:
  ```json
  [
    {
      "referer": "https://www.google.com/",
      "year": 2025,
      "count": 5
    },
    {
      "referer": "direct",
      "year": 2025,
      "count": 2
    }
  ]
  ```

애플리케이션을 실행하기 전, 사용 중인 운영체제에 맞춰 아래 가이드를 따라 환경 변수를 설정해 주세요. (예시 비밀번호: `aaa`)

### Windows

**1. Command Prompt (CMD)**

현재 세션에만 적용됩니다. 새 터미널을 열 때마다 다시 설정해야 합니다.

```bash
cmd set JASYPT_ENCRYPTOR_PASSWORD=aaa
```

**2. PowerShell**

현재 세션에만 적용됩니다.

```bash
powershell $env:JASYPT_ENCRYPTOR_PASSWORD="aaa"
```
**3. 영구적으로 설정**

1.  '시스템 환경 변수 편집'을 검색하여 실행합니다.
2.  '환경 변수...' 버튼을 클릭합니다.
3.  '새로 만들기...'를 클릭하여 변수 이름에 `JASYPT_ENCRYPTOR_PASSWORD`, 변수 값에 `aaa`를 입력하고 확인합니다.

### Linux & macOS

현재 세션에만 적용됩니다.

```bash
bash export JASYPT_ENCRYPTOR_PASSWORD=aaa
```
**영구적으로 설정**하려면, 사용 중인 셸의 설정 파일(`~/.bashrc`, `~/.zshrc`, `~/.profile` 등) 맨 아래에 위 `export` 명령어를 추가한 후, 터미널을 다시 시작하거나 `source ~/.bashrc` 와 같이 파일을 다시 로드합니다.

---

## API 호출 흐름 (API Call Flow)

본 시스템은 크게 **API 키 발급(회원가입)**과 **API 인증 및 사용** 두 단계로 구성됩니다. 인증은 JWT(JSON Web Token)를 기반으로 이루어집니다.

### 1단계: API 키 발급 (회원가입 + 토큰 요청)

이 단계는 사용자가 시스템에 처음 등록하고, 별도의 토큰 발급 요청을 통해 API Key/Refresh Token 쌍을 획득하는 과정입니다.

1.  **회원가입 요청**:
    *   사용자는 자신의 `username`을 포함하여 `/api/auth/register` 엔드포인트로 `POST` 요청을 전송합니다.
    *   컨트롤러는 `UserService.createUser`를 호출해 중복 여부를 확인하고 사용자를 저장합니다.

2.  **토큰 발급 요청**:
    *   등록이 완료되면, 클라이언트는 `/api/auth/token/issue` 엔드포인트에 동일한 `username`을 전달해 토큰 발급을 요청합니다.

3.  **토큰 생성 (`TokenService`)**:
    *   `TokenService.issueToken`은 사용자 존재 여부를 확인한 뒤 `JwtProvider.createToken`을 호출해 Access Token을 생성합니다.
    *   동시에 새 Refresh Token(UUID 기반)을 만들어 `User` 엔티티에 함께 저장합니다.

4.  **응답 반환**:
    *   컨트롤러는 발급된 Access Token과 Refresh Token을 응답 본문으로 돌려줍니다.
    *   이후 클라이언트는 Access Token을 API 호출에, Refresh Token은 재발급 요청(`/token/re-issue`)에 사용합니다.

### 2단계: API 인증 및 사용

사용자가 API 키를 발급받은 후, 이 키를 사용하여 시스템의 다른 기능(예: 단축 URL 생성)을 사용하는 과정입니다.

1.  **클라이언트 요청**:
    *   사용자는 보호된 API(예: `/api/short-urls`)를 호출할 때, HTTP 요청의 `Authorization` 헤더에 발급받은 API 키를 `Bearer` 타입으로 포함하여 전송합니다.
    *   **예시**: `Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...`

2.  **인증 필터 (`JwtAuthenticationFilter`)**:
    *   요청은 컨트롤러에 도달하기 전에 `JwtAuthenticationFilter`에 의해 가로채입니다.
    *   이 필터는 `Authorization` 헤더에서 JWT(API 키)를 추출합니다.

3.  **토큰 유효성 검증**:
    *   필터는 추출한 JWT를 `JwtProvider.validateToken` 메소드로 전달하여 유효성을 검증합니다.
    *   `JwtProvider`는 토큰을 파싱할 때 1단계에서 사용했던 것과 **동일한 비밀 키**를 사용하여 서명이 올바른지 확인합니다. 만약 서명이 일치하지 않거나 토큰이 변조되었다면 검증은 실패합니다.

4.  **사용자 인증 처리**:
    *   토큰이 유효하면, `JwtProvider.getUsernameFromToken`을 통해 토큰에서 `username`을 추출하고, 이를 기반으로 사용자 정보를 조회하여 Spring Security 컨텍스트에 인증 정보를 저장합니다.

5.  **컨트롤러 로직 실행**:
    *   인증이 완료되면, 요청은 본래 목적지였던 컨트롤러로 전달되어 비즈니스 로직을 수행합니다.

## 테스트 스크립트 실행

프로젝트 루트 경로에 포함된 `url_shortener_script.sh` 셸 스크립트를 사용하여 API의 전체 흐름(회원가입, 단축 URL 생성, 리디렉션)을 테스트할 수 있습니다.

> **주의:** 스크립트를 실행하기 전에 반드시 [환경 변수 설정](#환경-변수-설정-jasypt-encryptor-password)을 완료해야 합니다.

터미널에서 아래 명령어를 실행하세요.

**1. 스크립트에 실행 권한 부여 (최초 1회)**

```bash
chmod +x ./url_shortener_script.sh
```

**2. 스크립트 실행**

```bash
./url_shortener_script.sh
```

스크립트가 정상적으로 실행되면, API 키 발급부터 단축 URL 생성 및 최종 리디렉션까지의 과정이 순차적으로 수행됩니다.
