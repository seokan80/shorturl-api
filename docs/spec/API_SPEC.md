# API 엔드포인트 개요

컨트롤러별 명세를 개별 문서로 분리했습니다. 이 파일은 공통 규칙과 문서 인덱스를 제공합니다.

## 공통 규칙
- **기본 URL**: `https://{host}` (로컬 개발 기본값 `http://localhost:8080`)
- **응답 형태**: 모든 REST 엔드포인트는 `ResultEntity` 포맷을 사용하며 공통적으로
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": { ... } // 엔드포인트에 따라 객체/원시값/배열/null
  }
  ```
  구조를 따릅니다.
- **상태 코드/메시지**: `code`와 `message` 값은 `ApiResult` 열거형을 기준으로 합니다. 대표 코드:
  - `0000` 성공, `1401` 인증 오류, `1403` 권한 없음, `1404` 리소스 없음, `9999` 일반 실패.
- **인증 헤더**
  - 대부분의 보호된 엔드포인트: `Authorization: Bearer {API_KEY}`
  - 등록/토큰 발급: `X-CLIENTACCESS-KEY`
- **문자 인코딩**: JSON 본문은 UTF-8 인코딩을 사용합니다.

## 컨트롤러별 상세 문서
- [AuthController 명세](auth-controller.md)
- [ShortUrlController 명세](short-url-controller.md)
- [ShortUrlRedirectController 명세](short-url-redirect-controller.md)
- [RedirectionHistoryController 명세](redirection-history-controller.md)
