# WorkNote Backend - current integrated source

이 소스는 사용자가 업로드한 최신 `demo.zip` 백엔드에 메모 이미지 OCR 백엔드 패치를 합친 현재 통합본이다.

## 현재 Backend에 포함된 기능

- Spring Boot / Java 21
- JWT Stateless 인증
- PostgreSQL / Supabase 연결 설정
- WorkLog CRUD 및 기존 Ollama 분석
- Dashboard / AI Report / PDF
- Swagger/OpenAPI
- 메모 이미지 -> Azure AI Document Intelligence OCR -> Ollama -> WorkLog Draft

## 메모 OCR 신규/수정 핵심 파일

- `src/main/java/com/example/demo/ocr/client/OcrClient.java`
- `src/main/java/com/example/demo/ocr/client/AzureDocumentIntelligenceOcrClient.java`
- `src/main/java/com/example/demo/work/controller/WorkLogDraftController.java`
- `src/main/java/com/example/demo/work/service/WorkLogDraftService.java`
- `src/main/java/com/example/demo/work/response/WorkLogDraftResponse.java`
- `src/main/java/com/example/demo/ai/service/OllamaService.java`
- `src/main/resources/application.yaml`
- `.env.example`

## OCR API

`POST /api/work/draft/from-image`

multipart form:
- `image`: JPG/JPEG/PNG
- `language`: `ko` 또는 `ja`

응답:
- `title`
- `content`
- `recognizedText`

이 API는 Draft만 생성하며 DB에 직접 저장하지 않는다. 사용자가 Frontend에서 확인/수정한 뒤 기존 WorkLog 저장 API를 사용한다.

## 배포 환경변수

로컬은 `.env` / `.env.local`에서 실제 값을 관리하고, Render에서는 동일한 값을 Environment Variables로 등록한다.
`.env.example`에는 placeholder만 포함되어 있다.

OCR 관련 주요 값:

- `OCR_AZURE_ENDPOINT`
- `OCR_AZURE_API_KEY`
- `OCR_AZURE_API_VERSION`
- `OCR_AZURE_MODEL_ID`
- `OCR_AZURE_TIMEOUT_SECONDS`
- `OCR_AZURE_POLL_INTERVAL_MILLIS`
- `OCR_MAX_FILE_SIZE`
- `OCR_MAX_REQUEST_SIZE`
- `OCR_MAX_FILE_SIZE_BYTES`
- `OCR_MAX_RECOGNIZED_TEXT_CHARS`

## 로그인 2분 30초 진행률 변경과의 관계

최근 추가한 로그인 Cold Start 진행률 / 10분 timeout 처리는 Frontend 전용 변경이다.
따라서 Backend는 이 OCR 통합본 이후 추가 변경이 없다.

## 검증 메모

현재 실행 환경에서는 Gradle 9.5.1 distribution 다운로드가 네트워크 차단으로 실패하여 전체 `./gradlew compileJava`를 다시 실행할 수 없었다.
업로드된 기존 fat jar의 dependency를 이용한 부분 컴파일에서는 OCR Service/Client/DTO 등 핵심 Java 소스는 확인할 수 있었으나, 기존 fat jar가 현재 Swagger dependency보다 오래된 산출물이라 신규 Controller까지 동일 방식으로 완전 검증할 수는 없었다.
실제 로컬 환경에서는 인터넷 연결 상태에서 `./gradlew clean build`로 최종 검증하는 것이 필요하다.

## 중요

PDF용 실제 폰트 바이너리는 이 전달 ZIP에 포함하지 않았다. 기존 프로젝트의 `src/main/resources/fonts`는 로컬에서 그대로 유지해야 한다.
