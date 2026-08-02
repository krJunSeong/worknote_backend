# WorkNote 한국어/일본어 PDF 폰트 분리 수정

## 포함 파일

```text
src/main/java/com/example/demo/report/service/PdfReportService.java
src/main/resources/application.yaml
src/main/resources/fonts/README.txt
```

## 적용 구조

```text
src/main/resources/fonts/
├─ NanumGothic.ttf   # 한국어
└─ ipaexg.ttf        # 일본어(IPAex Gothic)
```

폰트 파일은 저작권 및 배포 조건 확인이 필요하므로 압축에 포함하지 않았습니다.

## 동작 방식

- 기존 `createPdf(report)` 호출을 그대로 사용할 수 있습니다.
- 보고서 본문에 히라가나/가타카나가 있으면 일본어 보고서로 판단합니다.
- 일본어 보고서는 `ipaexg.ttf`, 한국어 보고서는 `NanumGothic.ttf`를 사용합니다.
- PDF 제목, 구역명, 통계 단위도 한국어/일본어로 구분됩니다.
- 일본어 보고서에서 DB 난이도 키가 `초급/중급/고급`으로 들어와도 `初級/中級/上級`으로 변환합니다.
- 클래스패스와 일반 파일 시스템 경로를 모두 지원합니다.

## application.yaml

```yaml
report:
  pdf:
    korean-font-path: ${REPORT_PDF_KOREAN_FONT_PATH:classpath:fonts/NanumGothic.ttf}
    japanese-font-path: ${REPORT_PDF_JAPANESE_FONT_PATH:classpath:fonts/ipaexg.ttf}
```

## Render 환경변수

기존 `REPORT_PDF_FONT_PATH`는 더 이상 사용하지 않습니다. 삭제하거나 무시하고 다음 값을 사용하세요.

```text
REPORT_PDF_KOREAN_FONT_PATH=classpath:fonts/NanumGothic.ttf
REPORT_PDF_JAPANESE_FONT_PATH=classpath:fonts/ipaexg.ttf
```

## 빌드 전 확인

```bash
./gradlew clean bootJar
jar tf build/libs/*.jar | grep "BOOT-INF/classes/fonts"
```

다음 두 파일이 보여야 합니다.

```text
BOOT-INF/classes/fonts/NanumGothic.ttf
BOOT-INF/classes/fonts/ipaexg.ttf
```

## 배포

```bash
git add .
git commit -m "Split Korean and Japanese PDF fonts"
git push origin main
```

그다음 Render에서 최신 커밋을 다시 배포하세요.
