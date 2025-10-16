# 단축 URL 서비스 API

이 문서는 단축 URL 서비스의 API 사용법과 전체적인 인증 흐름을 설명합니다.

## 환경 변수 설정 (Jasypt Encryptor Password)

애플리케이션은 `application.yml` 파일의 민감한 정보(예: 데이터베이스 비밀번호, JWT 시크릿 키)를 암호화하기 위해 Jasypt를 사용합니다. 암호화된 값을 복호화하기 위해서는 마스터 키 역할을 하는 `JASYPT_ENCRYPTOR_PASSWORD` 환경 변수를 설정해야 합니다.

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

### 1단계: API 키 발급 (회원가입)

이 단계는 사용자가 시스템에 처음 등록하고, API를 사용하는 데 필요한 고유한 키(JWT)를 발급받는 과정입니다.

1.  **클라이언트 요청**:
    *   사용자는 자신의 `username`을 포함하여 회원가입을 요청합니다.
    *   HTTP `POST` 요청이 `/api/auth/register` 엔드포인트로 전송됩니다.

2.  **컨트롤러 처리 (`AuthController`)**:
    *   `register` 메소드가 이 요청을 받아 `UserService`의 `createUser` 메소드를 호출하여 사용자 생성을 위임합니다.

3.  **서비스 로직 (`UserService`)**:
    *   `createUser` 메소드는 먼저 동일한 `username`의 존재 여부를 확인합니다.
    *   `JwtProvider.createToken` 메소드를 호출하여 해당 사용자를 위한 고유 API 키(JWT)를 생성합니다.

4.  **JWT 생성 (`JwtProvider`)**:
    *   `createToken` 메소드는 `username`을 주제(subject)로 하고, 설정 파일(`application.yml`)에 정의된 만료 시간을 적용하여 JWT를 생성합니다.
    *   가장 중요한 부분으로, `HS512` 알고리즘과 **안전하게 생성된 비밀 키(`jwt.secret`)**를 사용하여 토큰에 전자 서명을 합니다. 이 서명은 토큰이 위변조되지 않았음을 보장합니다.

5.  **사용자 정보 저장 및 응답**:
    *   `UserService`는 생성된 JWT(API 키)와 `username`을 `User` 엔티티에 담아 데이터베이스에 저장합니다.
    *   마지막으로, `AuthController`는 생성된 사용자의 `username`과 발급된 API 키를 클라이언트에게 응답으로 반환합니다.

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