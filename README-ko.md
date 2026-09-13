# WorkNote Backend

Spring Boot 기반 WorkNote 백엔드 통합본입니다. JWT 인증, 업무일지, AI 분석, OCR, Dashboard, Calendar, Goal Planner, AI Report/PDF를 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.4
- Spring Web / WebFlux
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- Swagger/OpenAPI
- Apache PDFBox
- Azure AI Document Intelligence OCR
- Ollama AI 분석

## 핵심 기능

### 인증 / 입력 검증

- 로그인 ID: 4~20자, 영문/숫자/밑줄(_)만 허용
- 신규 비밀번호: 8~64자, 특수문자/공백 허용, BCrypt 저장
- 로그인 비밀번호 입력: 기존 계정 호환을 위해 최대 64자만 제한
- 닉네임: 2~12자, 문자/숫자/공백/밑줄(_) 허용
- 운영 JWT Secret은 `JWT_SECRET` 환경변수 필수
- local 프로필만 개발용 Secret fallback 사용

### 업무일지

```text
POST   /api/work
GET    /api/work/{userId}
GET    /api/work/detail/{id}
PUT    /api/work/{id}
PATCH  /api/work/{id}/date
DELETE /api/work/{id}
```

`PATCH /api/work/{id}/date`는 캘린더 Drag & Drop용 API이며 AI를 다시 호출하지 않습니다.

업무 날짜는 `workDate`, 최초 생성 시각은 `createdAt`으로 분리합니다.

```text
createdAt = 감사/생성 이력
workDate  = 캘린더에서 사용하는 업무 날짜
```

기존 데이터에서 `workDate`가 null이면 `createdAt.toLocalDate()`를 사용합니다.

### 캘린더

```text
GET /api/calendar?year=2026&month=9
```

- JWT 로그인 사용자 데이터만 반환
- 업무일지 `workDate` 월간 조회
- 해당 월과 기간이 겹치는 목표 반환
- Goal `startDate ~ targetDate` 기간 표시 지원

### 계획 목표

```text
GET    /api/goals
POST   /api/goals
PUT    /api/goals/{id}
PATCH  /api/goals/{id}/schedule
PATCH  /api/goals/{id}/progress
DELETE /api/goals/{id}
```

- `startDate`: 계획 시작일
- `targetDate`: 계획 마감일
- `status`: PLANNED / IN_PROGRESS / COMPLETED
- `progress`: 0~100
- 진행률 0 → PLANNED
- 진행률 1~99 → IN_PROGRESS
- 진행률 100 → COMPLETED
- 미완료 상태에서 마감일 경과 시 overdue
- 프론트에서는 완료 목표를 캘린더에서 체크/취소선으로 표시
- 제목/설명 인라인 수정은 기존 소유권 검증이 적용되는 Goal 수정 흐름을 사용

기존 Goal에 `startDate`가 없으면 `targetDate`를 시작일로 간주합니다.

### 대시보드 권한

현재 권장 API:

```text
GET /api/dashboard
```

클라이언트가 userId를 전달하지 않습니다. 서버가 JWT의 loginId로 사용자를 찾고 해당 사용자의 데이터만 조회합니다.

구버전 호환 API `/api/dashboard/{userId}`도 존재하지만 요청 ID가 JWT 사용자와 일치하지 않으면 403입니다.

### AI / Azure OCR 일일 제한

DB에 사용자별 일일 사용량을 기록합니다.

```text
app.usage-limits.ai-per-user-per-day=20
app.usage-limits.azure-ocr-per-user-per-day=5
```

이미지 초안 생성은 OCR 1회 + AI 1회를 동시에 소비합니다.

초과 응답:

```text
HTTP 429
code: DAILY_AI_LIMIT_EXCEEDED
message: 오늘 쓸 수 있는 AI기능을 다 썼습니다. 내일 다시 시도해주세요.
```

## DB 스키마 추가 사항

Hibernate `ddl-auto=update` 개발 설정에서는 다음 컬럼이 자동 추가됩니다.

```text
work_logs.work_date
goals.start_date
```


## 로컬 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

운영에서는 local 프로필을 사용하지 말고 `JWT_SECRET`을 환경변수로 설정합니다.
