# WorkNote Backend - Integrated Feature Notes

WorkNote 백엔드의 현재 통합 기능과 설계 기준을 정리한 문서입니다.

## Runtime

- **Java 21** (Gradle Toolchain)
- Spring Boot 3.5.4
- PostgreSQL / Spring Data JPA / Hibernate
- Spring Security + JWT + BCrypt
- Ollama AI / Azure AI Document Intelligence OCR
- Apache PDFBox

## Authentication / Validation

- 로그인 ID: 4~20자, 영문/숫자/밑줄 allowlist
- 회원가입 비밀번호: 8~64자
- 닉네임: 2~12자, 문자/숫자/공백/밑줄 allowlist
- 잘못된 로그인 정보: `401 INVALID_CREDENTIALS`
- 예상하지 못한 예외: 내부 exception message/stack trace를 응답에 노출하지 않고 일반화된 `500 INTERNAL_SERVER_ERROR` 반환
- 운영 JWT Secret은 환경변수 필수

## WorkLog

- CRUD + 상세조회
- AI 요약 / 기술 태그 / 난이도 / 예상 면접 질문
- `createdAt`(최초 생성 이력)과 `workDate`(업무 날짜) 분리
- Calendar Drag & Drop은 `workDate`만 변경

## Calendar

- JWT 사용자 기준 월간 데이터 조회
- WorkLog 날짜 이동
- 날짜 더블클릭 빠른 WorkLog 작성
- Goal 기간 표시, 이동, 시작/종료 Resize
- 완료 Goal 상태/진행률 제공

## Goal Planner

- `startDate ~ targetDate` 기간형 목표
- `PLANNED / IN_PROGRESS / COMPLETED`
- 0~100 진행률 및 overdue 계산
- schedule/progress 부분 업데이트
- 제목/설명 인라인 편집 시에도 소유권 검증 유지

## Ownership / IDOR Protection

- Dashboard는 JWT 로그인 사용자를 서버에서 직접 식별
- WorkLog/Goal 상세·수정·삭제는 로그인 사용자 소유 데이터만 허용
- 클라이언트에서 전달되는 식별값을 권한 판단의 신뢰 기준으로 사용하지 않음

## AI / OCR Usage Limit

- DB에 `user + date + feature` 단위로 일일 사용량 저장
- AI 20회/일/사용자
- Azure OCR 5회/일/사용자
- 초과 시 `429 DAILY_AI_LIMIT_EXCEEDED`

## OCR Security

- 허용 이미지 형식 제한
- MIME + 실제 이미지 포맷 이중 검사
- 파일 크기/해상도/OCR 텍스트 길이 제한
- OCR 후 AI 업무일지 초안 생성

## Report / PDF

- 누적 WorkLog 기반 AI 보고서
- PDFBox 출력
- 한국어/일본어 지원
