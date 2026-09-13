# WorkNote Backend

WorkNote의 REST API, 인증/권한, 업무 기록, AI 분석, OCR, Calendar, Goal Planner, Dashboard, AI Report/PDF를 담당하는 Spring Boot 백엔드입니다.

## 실행 환경 / 기술 스택

| 항목 | 사용 기술 |
| --- | --- |
| Language | **Java 21** |
| Framework | Spring Boot 3.5.4 |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| AI | Ollama |
| OCR | Azure AI Document Intelligence |
| PDF | Apache PDFBox |
| API Docs | Springdoc OpenAPI / Swagger |
| Build | Gradle |

### Java 버전

프로젝트는 Gradle Toolchain에서 **Java 21**을 사용하도록 고정되어 있습니다.

```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

로컬 실행 환경에서도 JDK 21 사용을 권장합니다.

## 주요 기능

### 1. 인증 / 사용자 입력 검증

- JWT 기반 Stateless 인증
- BCrypt 비밀번호 해시 저장
- 로그인 ID: 4~20자, 영문/숫자/밑줄(`_`)만 허용
- 신규 비밀번호: 8~64자
- 닉네임: 2~12자, 문자/숫자/공백/밑줄 허용
- 로그인 실패는 `401 INVALID_CREDENTIALS`로 통일
- 예상하지 못한 서버 예외는 내부 상세 내용을 노출하지 않고 일반화된 500 응답 반환
- 운영 환경의 JWT Secret은 `JWT_SECRET` 환경변수 필수

### 2. 업무일지 / AI 분석

- 업무일지 등록·조회·상세조회·수정·삭제
- 업무 작성 시 AI가 요약, 기술 태그, 난이도, 예상 면접 질문 생성
- `createdAt`과 `workDate`를 분리해 최초 작성 이력과 실제 업무 날짜를 별도로 관리
- 캘린더 Drag & Drop으로 `workDate`만 변경하므로 최초 작성 시각은 유지

### 3. Calendar

- 월간 업무일지와 계획 목표를 한 화면에서 조회
- 업무일지 Drag & Drop 날짜 이동
- 날짜 더블클릭으로 선택한 날짜에 업무일지 바로 작성
- 계획 목표의 기간 이동 및 시작일/마감일 Resize 지원
- 완료 목표는 프론트에서 체크 표시와 취소선으로 구분할 수 있도록 상태/진행률 제공
- Calendar 조회는 클라이언트의 `userId`를 받지 않고 JWT 로그인 사용자를 기준으로 처리

### 4. Goal Planner

- 목표 제목/설명, 시작일, 마감일, 상태, 진행률 관리
- 상태: `PLANNED`, `IN_PROGRESS`, `COMPLETED`
- 진행률 0~100 저장 및 진행률에 따른 상태 자동 반영
- 마감일이 지난 미완료 목표는 overdue 계산
- 일정 이동/Resize 전용 schedule 변경 처리
- 진행률 전용 업데이트 처리
- 제목/설명 인라인 편집도 기존 Goal 소유권 검증을 거쳐 저장

### 5. Dashboard 보안

Dashboard의 사용자 식별은 클라이언트가 전달한 값에 의존하지 않고 JWT 인증 정보에서 결정합니다.

- 로그인 사용자 데이터만 집계
- 다른 사용자의 Dashboard/Goal/WorkLog 접근 시 소유권 검사
- 구버전 호환 경로도 요청 사용자와 JWT 사용자가 다르면 차단

### 6. 사용자별 AI / OCR 일일 제한

사용량을 DB에 `사용자 + 날짜 + 기능` 단위로 저장합니다.

기본값:

```text
AI         20회 / 일 / 사용자
Azure OCR   5회 / 일 / 사용자
```

한도를 초과하면 `429 DAILY_AI_LIMIT_EXCEEDED`를 반환합니다. 이미지 기반 업무 초안 생성은 OCR 1회와 AI 1회를 함께 사용합니다.

### 7. Azure OCR 이미지 처리

- JPG/JPEG/PNG 허용
- MIME 타입뿐 아니라 실제 이미지 포맷을 서버에서 재검증
- 파일 크기 및 이미지 해상도 제한
- OCR 결과 길이 제한
- OCR 텍스트를 이용해 업무일지 초안 생성
- 원본 이미지와 OCR 원문은 WorkNote DB에 영구 저장하지 않음

### 8. AI Report / PDF

- 누적 업무 기록 기반 AI 보고서 생성
- 기술 경험과 업무 기록을 보고서 형태로 정리
- PDFBox 기반 PDF 출력
- 한국어/일본어 출력 지원

## 데이터 모델에서 추가된 주요 필드

```text
work_logs.work_date  : 캘린더에서 사용하는 업무 날짜
goals.start_date     : 계획 목표 시작일
goals.target_date    : 계획 목표 마감일
goals.progress       : 목표 진행률
goals.status         : 목표 상태
```

기존 데이터에서 `workDate`가 없으면 생성일을 업무 날짜로 사용하고, 기존 목표에서 `startDate`가 없으면 `targetDate`를 시작일로 간주합니다.

## 로컬 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

기본 포트는 `8081`입니다. 운영 환경에서는 `local` 프로필을 사용하지 않고 필요한 DB/JWT/OCR/AI 설정을 환경변수로 전달합니다.
