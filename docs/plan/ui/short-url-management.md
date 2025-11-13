# 단축 URL 관리 UI 설계

## 1. 목적
- 기존 `단축 URL 제어` 카드형 화면을 폐기하고, 운영자가 실제 단축 URL 데이터를 CRUD(등록/조회/수정/삭제) 방식으로 관리할 수 있는 CMS 화면을 제공한다.
- 백엔드 `ShortUrlController`에서 제공하는 실 API와 연동하여 목록 페이징, 상세 정보, 만료일 수정, 삭제 권한 검증을 포함한다.

## 2. 요구사항 정리
1. 내비게이션·페이지 타이틀을 `단축 URL 관리`로 변경한다.
2. 목록
   - `/api/short-url?page={n}&size={m}&sort=createdAt,desc` 호출.
   - 결과 `ResultEntity<ResultList<ShortUrlResponse>>`를 파싱하여 `totalCount`, `elements` 배열을 테이블로 렌더링.
   - 기본 페이지 크기 10, 페이지네이터 제공.
3. 등록
   - `POST /api/short-url`에 `{ longUrl }` 전달.
   - 성공 시 목록 최상단에 삽입 후 상태 배너 출력.
4. 삭제
   - `DELETE /api/short-url/{id}` 로 변경하고, 컨트롤러/서비스에서 `principal.getName()` 기반 등록자 체크.
5. 상세/수정
   - `GET /api/short-url/{id}`로 상세 로딩.
   - 만료일만 수정 가능: `PUT /api/short-url/{id}/expiration` + `{ expiredAt: "yyyy-MM-dd'T'HH:mm:ss" }`.
6. 삭제/만료 수정은 로그인 사용자(등록자)가 아닌 경우 `403`/`FORBIDDEN`을 내려 UI에서 에러 배너로 노출.

## 3. 화면 구성
- **상단 카드 (신규 등록)**
  - 단일 입력 `원본 URL` + 생성 버튼/초기화 버튼.
  - 제출 중 로딩 스피너, 성공 시 입력 초기화.
- **목록 카드**
  - `Table` 컴포넌트 활용, 컬럼: Short Key, 원본 URL, 생성자, 생성일, 만료일, 상태(만료 여부), 작업.
  - 작업 버튼: `상세`(아이콘) · `삭제`.
  - 페이지 하단에 간단 페이지네이터(이전/다음, 현재 페이지 표시).
- **상세/수정 패널**
  - 목록에서 항목 선택 시 하단 카드가 열려 세부 정보 표시.
  - 만료일은 `datetime-local` 입력에 ISO 포맷으로 양방향 매핑.
  - 저장 버튼 클릭 시 `PUT /{id}/expiration`.
  - 카드 내에서 `shortUrl` 복사 버튼, 생성/만료 메타 정보 제공.
- **상태 배너**
  - `ClientAccessKeyPage`와 동일한 패턴을 재사용하여 성공/실패 메시지를 공통으로 처리.

## 4. 상태 및 데이터 모델
```ts
type ShortUrlItem = {
  id: number;
  shortKey: string;
  shortUrl: string;
  longUrl: string;
  createdBy: string;
  userId: number;
  createdAt: string;
  expiredAt: string | null;
};

type ShortUrlList = {
  totalCount: number;
  elements: ShortUrlItem[];
};
```
- `ApiEnvelope<T>` 재정의 (code/message/data) 후, 재사용 가능한 `request` 유틸을 페이지 내부에 선언.
- 페이지 상태:
  - `items`, `pagination = { page: 0, size: 10, total: 0 }`
  - `selected: ShortUrlItem | null`
  - `createForm = { longUrl: "" }`
  - `editForm = { expiredAt: "" }`
  - `busyAction`, `statusBanner`

## 5. API 연동 및 백엔드 변경
| 액션 | 메서드 & 경로 | 파라미터 | 응답 처리 |
| --- | --- | --- | --- |
| 목록 조회 | `GET /api/short-url?page=&size=&sort=` | 쿼리 문자열 | `data.totalCount`, `data.elements` |
| 단건 조회 | `GET /api/short-url/{id}` | path | 상세 카드 초기값 |
| 등록 | `POST /api/short-url` | `{ longUrl }` | 새 항목 prepend |
| 만료일 수정 | `PUT /api/short-url/{id}/expiration` | `{ expiredAt }` | `selected` 및 `items` 업데이트 |
| 삭제 | `DELETE /api/short-url/{id}` | path, Principal 필수 | 목록에서 제거 |

백엔드 조정:
1. `ShortUrlController.delete` → `@DeleteMapping("/{id}")`, `Principal` 주입.
2. `ShortUrlService.deleteShortUrl(Long, String username)` 시그니처 변경 후, 생성자와 이름 비교하여 불일치 시 `IllegalStateException` 던짐.
3. UI는 `403`과 `code !== "0000"`을 구분해 메시지를 노출.

## 6. 구현 단계
1. **UI 라우팅/레이블 갱신**
   - `AdminSidebar` 라벨, `ShortUrlControlsPage` 교체 (파일명 `ShortUrlManagementPage`).
2. **페이지 컴포넌트 작성**
   - hooks: `useEffect`로 목록 로딩, `useMemo`로 페이징 버튼 비활성화.
   - 테이블/폼/상세 카드 컴포넌트화(동일 파일 내부의 소형 컴포넌트)로 가독성 유지.
3. **백엔드 권한 체크 반영**
   - 서비스/컨트롤러 수정 + 예외 메시지.
4. **테스트/검증**
   - `yarn test --filter ShortUrlManagementPage` (추가 시) 또는 lint.
   - 백엔드: `./gradlew test` (옵션) 또는 관련 단위 테스트 보완.

## 7. 고려 사항
- 만료일 수정 시 클라이언트 타임존 → ISO 포맷 `new Date(value).toISOString()` 으로 전송.
- 목록 응답의 `createdAt`은 `LocalDateTime`; UI에서 `new Date(value).toLocaleString()` 처리 전, null 체크 필요.
- 삭제/수정 시 optimistic update: UI에서 먼저 제거/적용 후 실패 시 롤백보단 목록 재요청으로 단순화.
- 추후 무한 스크롤 확장 가능하도록 pagination 상태를 hook 수준에서 분리할 수 있게 설계.
