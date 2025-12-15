# Docker 빌드 & 실행 가이드

단축 URL 서비스는 **멀티 컨테이너 구성**(admin-api, redirect, admin-ui + nginx)과 **올인원 단일 컨테이너 구성**(nginx + 두 Spring 앱 + 빌드된 UI)을 모두 지원합니다. Docker Compose 버전은 기존과 동일하게 `3.9`를 유지합니다.

## 공통 준비사항
- Docker / Docker Compose(v2) 설치
- 빌드 시점에 필요한 프로파일: `local`(기본), `dev`, `prod` 등
- Jasypt 마스터 키는 운영/개발 환경에서 `JASYPT_ENCRYPTOR_PASSWORD` 환경변수로 전달

### 빌드 스크립트 개요
`docker/build.sh [PROFILE] [IMAGE_TAG] [TARGET]`
- `PROFILE`(기본: `local`): Gradle 빌드 및 Spring 실행 프로파일
- `IMAGE_TAG`(기본: `latest`): 빌드 산출 이미지 태그
- `TARGET`(기본: `multi`): `multi` 또는 `all-in-one`

## 1) 멀티 컨테이너(기존) 빌드 & 실행
각 서비스가 별도 컨테이너로 동작하며, nginx는 admin-ui 정적 파일을 서빙하고 `/api`, `/r` 요청을 백엔드로 프록시합니다.

```bash
# 이미지 빌드 (multi 모드, 프로파일=local, 태그=latest)
./docker/build.sh local latest multi

# 컨테이너 기동
docker compose -f docker/docker-compose.yml up -d
```

노출 포트
- admin-api: `8080`
- redirect: `8081`
- admin-ui/nginx: `80`

주요 환경변수(기본값)
- `SPRING_PROFILES_ACTIVE` (`local`)
- `JASYPT_ENCRYPTOR_PASSWORD` (`shortUrlApi`)
- `SHORT_URL_REDIRECT_API_BASE_URL` (`http://redirect:8081`)
- `SHORT_URL_ADMIN_API_BASE_URL` (`http://admin-api:8080`)

## 2) 올인원 단일 컨테이너 빌드 & 실행
하나의 컨테이너에서 nginx, admin-api, redirect, 빌드된 admin-ui를 함께 실행합니다. supervisord가 프로세스를 관리합니다.

```bash
# 이미지 빌드 (all-in-one 모드)
./docker/build.sh local latest all-in-one

# 컨테이너 기동
docker compose -f docker/docker-compose.all-in-one.yml up -d
```

노출 포트
- nginx + UI + 프록시: `80`

주요 환경변수(기본값)
- `SPRING_PROFILES_ACTIVE` (`local`)
- `JASYPT_ENCRYPTOR_PASSWORD` (`shortUrlApi`)
- `SHORT_URL_REDIRECT_API_BASE_URL` (`http://127.0.0.1:8081`)
- `SHORT_URL_ADMIN_API_BASE_URL` (`http://127.0.0.1:8080`)

> 운영 배포 시에는 적절한 프로파일과 암호화 키, 외부 도메인/포트에 맞는 베이스 URL을 설정하세요.
