# WorkNote Backend - Integrated Feature / Security Notes

현재 통합본은 WorkNote의 인증, 업무 기록, AI/OCR, Dashboard, Calendar, Goal Planner, Report 기능을 하나의 Spring Boot 프로젝트로 합친 버전이다.

## 1. Authentication

- JWT Stateless authentication
- BCrypt password hashing
- Backend validation for loginId/password/nickname
  - loginId: 4~20, 영문/숫자/밑줄(_) allowlist
  - signup password: 8~64, 특수문자/공백 허용
  - nickname: 2~12, 문자/숫자/공백/밑줄(_) allowlist
- Production JWT secret has no hard-coded fallback
- Local-only fallback is isolated in `application-local.yaml`

## 2. WorkLog

WorkLog는 원본 생성 시각과 캘린더 날짜를 분리한다.

```text
createdAt : 최초 생성 시각. Drag & Drop으로 변경하지 않음.
workDate  : 사용자가 업무를 수행했다고 지정한 날짜.
```

API:

```text
POST   /api/work
GET    /api/work/{userId}
GET    /api/work/detail/{id}
PUT    /api/work/{id}
PATCH  /api/work/{id}/date
DELETE /api/work/{id}
```

`detail`, `update`, `delete`, `date` 변경 모두 JWT 소유권을 확인한다.

## 3. Calendar

```text
GET /api/calendar?year={year}&month={month}
```

Calendar API는 `userId`를 받지 않는다. 인증 컨텍스트의 사용자만 조회한다.

반환 데이터:

- WorkLog: id, title, workDate, createdAt
- Goal: id, title, startDate, targetDate, status, progress, overdue

Frontend 지원 동작:

- WorkLog Drag & Drop → `PATCH /api/work/{id}/date`
- Goal 이동 → 기간 길이를 유지한 채 `PATCH /api/goals/{id}/schedule`
- Goal 시작/끝 resize → 동일 schedule API
- 날짜 더블클릭 → 선택한 `workDate`로 새 WorkLog 생성
- 완료 Goal → 캘린더에서 체크 표시와 취소선으로 구분

## 4. Goal Planner

Goal은 단일 deadline에서 기간형 일정으로 확장되었다.

```text
startDate  : 시작일
targetDate : 종료일/마감일
```

API:

```text
GET    /api/goals
POST   /api/goals
PUT    /api/goals/{id}
PATCH  /api/goals/{id}/schedule
PATCH  /api/goals/{id}/progress
DELETE /api/goals/{id}
```

`PATCH /schedule`과 `PATCH /progress`는 전체 Goal 데이터를 클라이언트가 덮어쓰지 않고 필요한 필드만 변경한다.

프론트의 제목/설명 연필 아이콘 인라인 편집은 `PUT /api/goals/{id}`의 기존 소유권 검증과 길이 검증을 그대로 재사용한다.

## 5. Dashboard IDOR Protection

권장 API:

```text
GET /api/dashboard
```

서버가 JWT 사용자 ID를 직접 해석한다.

Backward compatibility:

```text
GET /api/dashboard/{userId}
```

요청 userId가 JWT 사용자와 다르면 403.

## 6. AI / OCR Usage Limit

사용량은 메모리가 아니라 DB에 다음 키로 저장한다.

```text
user + usageDate + feature
```

기본 제한:

```text
AI        20/day/user
AZURE_OCR  5/day/user
```

OCR Draft는 두 기능을 모두 소비한다.

## 7. OCR

```text
POST /api/work/draft/from-image
```

- JPG/JPEG/PNG
- 서버에서 실제 이미지 포맷 검사
- 파일 크기 / 이미지 크기 제한
- Azure AI Document Intelligence OCR
- OCR 텍스트를 Ollama에 전달하여 WorkLog draft 생성
- 원본 이미지와 OCR 원문은 WorkNote DB에 저장하지 않음

## 8. Report / PDF

- 누적 WorkLog 기반 AI 프로젝트 보고서
- PDFBox PDF 출력
- 한국어/일본어 폰트 분리
