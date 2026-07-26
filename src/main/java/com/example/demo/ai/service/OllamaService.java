package com.example.demo.ai.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.ai.dto.AiAnalysisResponse;
import com.example.demo.report.dto.AiReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public OllamaService(
            ObjectMapper objectMapper,

            @Value("${ollama.base-url:http://localhost:11434}")
            String baseUrl,

            @Value("${ollama.model:qwen2.5:3b}")
            String model,

            @Value("${ollama.api-key:}")
            String apiKey,

            @Value("${ollama.timeout-seconds:180}")
            long timeoutSeconds
    ) {

        this.objectMapper = objectMapper;
        this.model = model;
        this.timeout = Duration.ofSeconds(
                Math.max(timeoutSeconds, 1L)
        );

        WebClient.Builder builder =
                WebClient.builder()
                        .baseUrl(
                                removeTrailingSlash(baseUrl)
                        )
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .defaultHeader(
                                HttpHeaders.ACCEPT,
                                MediaType.APPLICATION_JSON_VALUE
                        );

        if (apiKey != null
                && !apiKey.isBlank()) {

            builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + apiKey.trim()
            );
        }

        this.webClient = builder.build();
    }

    public AiAnalysisResponse analyze(
            String workLog,
            String language
    ) {

        if (workLog == null
                || workLog.isBlank()) {

            throw new IllegalArgumentException(
                    "분석할 업무일지가 없습니다."
            );
        }

        String prompt = createPrompt(
                workLog,
                language
        );

        AiAnalysisResponse result =
                executeJsonRequest(
                        prompt,
                        AiAnalysisResponse.class
                );

        result.setDifficulty(
                normalizeDifficulty(
                        result.getDifficulty()
                )
        );

        return result;
    }

    public AiReportResponse generateReport(
            String reportData,
            String language
    ) {

        if (reportData == null
                || reportData.isBlank()) {

            throw new IllegalArgumentException(
                    "보고서 생성에 사용할 데이터가 없습니다."
            );
        }

        String prompt = createReportPrompt(
                reportData,
                language
        );

        return executeJsonRequest(
                prompt,
                AiReportResponse.class
        );
    }

    private <T> T executeJsonRequest(
            String prompt,
            Class<T> responseType
    ) {

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", "json",
                "options", Map.of(
                        "temperature", 0.1
                )
        );

        Map<String, Object> response;

        try {
            response =
                    webClient.post()
                            .uri("/api/generate")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            Map<String, Object>
                                    >() {
                                    }
                            )
                            .block(timeout);

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "AI 서버 요청에 실패했습니다. "
                            + "상태 코드: "
                            + e.getStatusCode().value()
                            + ", 응답: "
                            + safeResponseBody(
                                    e.getResponseBodyAsString()
                            ),
                    e
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "AI 서버에 연결하지 못했습니다. "
                            + "Ollama 주소, 모델명, API 키를 확인해 주세요.",
                    e
            );
        }

        if (response == null) {
            throw new RuntimeException(
                    "Ollama 응답이 없습니다."
            );
        }

        Object responseValue =
                response.get("response");

        if (responseValue == null) {
            throw new RuntimeException(
                    "Ollama 응답에 response 필드가 없습니다."
            );
        }

        try {
            return objectMapper.readValue(
                    responseValue.toString(),
                    responseType
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "AI 분석 결과를 JSON으로 처리하지 못했습니다.",
                    e
            );
        }
    }

    private String createPrompt(
            String workLog,
            String language
    ) {

        if ("ja".equalsIgnoreCase(language)) {
            return createJapanesePrompt(workLog);
        }

        return createKoreanPrompt(workLog);
    }

    private String createReportPrompt(
            String reportData,
            String language
    ) {

        if ("ja".equalsIgnoreCase(language)) {
            return createJapaneseReportPrompt(
                    reportData
            );
        }

        return createKoreanReportPrompt(
                reportData
        );
    }

    private String createKoreanPrompt(
            String workLog
    ) {

        return """
                당신은 한국인 개발자의 업무일지를 분석하는 AI입니다.

                반드시 다음 규칙을 지켜주세요.

                1. 모든 설명은 반드시 자연스러운 한국어로 작성합니다.
                2. 중국어와 일본어로 답변하지 않습니다.
                3. Spring Boot, React, PostgreSQL, Docker, Ollama 같은
                   기술 이름만 영문 표기를 사용할 수 있습니다.
                4. 반드시 아래 JSON 형식으로만 응답합니다.
                5. JSON 바깥에 설명이나 코드 블록을 추가하지 않습니다.
                6. summary는 업무의 핵심과 문제 해결 내용을 포함해
                   자연스러운 한국어 1~2문장으로 작성합니다.
                7. techTags는 실제 업무 내용에 등장하거나
                   직접 관련된 기술만 최대 5개 작성합니다.
                8. questions는 실제 개발자 면접에서 물어볼 수 있는
                   질문을 정확히 3개 작성합니다.
                9. difficulty는 초급, 중급, 고급 중 하나만 작성합니다.

                {
                  "summary": "업무 내용을 한국어로 1~2문장 요약",
                  "techTags": [
                    "사용 기술 1",
                    "사용 기술 2"
                  ],
                  "questions": [
                    "한국어 예상 면접 질문 1",
                    "한국어 예상 면접 질문 2",
                    "한국어 예상 면접 질문 3"
                  ],
                  "difficulty": "초급"
                }

                분석할 업무일지:

                %s
                """.formatted(workLog);
    }

    private String createJapanesePrompt(
            String workLog
    ) {

        return """
                あなたは日本企業のソフトウェアエンジニア採用面接官として、
                開発者の業務日誌を分析するAIです。

                必ず以下のルールを守ってください。

                1. すべての説明は自然な日本語で作成してください。
                2. 韓国語や中国語は使用しないでください。
                3. Spring Boot、React、PostgreSQL、Docker、Ollamaなどの
                   技術名は英語表記を使用しても構いません。
                4. 必ず以下のJSON形式のみで回答してください。
                5. JSONの外側に説明やコードブロックを追加しないでください。
                6. summaryは業務の要点と問題解決内容を含め、
                   自然な日本語で1～2文にまとめてください。
                7. techTagsは実際の業務内容に登場した、
                   または直接関係する技術のみ最大5件作成してください。
                8. questionsは日本企業の開発者面接で質問される可能性がある
                   内容を正確に3件作成してください。
                9. difficultyは初級、中級、上級のいずれか一つだけを
                   出力してください。

                {
                  "summary": "業務内容を日本語で1～2文に要約",
                  "techTags": [
                    "使用技術1",
                    "使用技術2"
                  ],
                  "questions": [
                    "日本語の想定面接質問1",
                    "日本語の想定面接質問2",
                    "日本語の想定面接質問3"
                  ],
                  "difficulty": "初級"
                }

                分析する業務日誌:

                %s
                """.formatted(workLog);
    }

    private String createKoreanReportPrompt(
            String reportData
    ) {

        return """
                당신은 개발자의 전체 업무 로그를 바탕으로
                최종 업무 보고서를 작성하는 AI입니다.

                반드시 다음 규칙을 지켜주세요.

                1. 모든 내용은 자연스러운 한국어로 작성합니다.
                2. 제공된 업무 로그와 통계만 사용합니다.
                3. 기록에 없는 기능이나 성과를 임의로 만들지 않습니다.
                4. 동일하거나 유사한 업무는 하나의 기능으로 묶습니다.
                5. 업무 로그를 단순히 나열하지 말고 전체 흐름을 종합합니다.
                6. 난이도 수치와 난이도별 개수는 입력값을 그대로 해석합니다.
                7. 향후 개선 사항은 현재 기록을 바탕으로 현실적으로 작성합니다.
                8. 반드시 아래 JSON 형식으로만 응답합니다.
                9. JSON 바깥에 설명, 마크다운, 코드 블록을 작성하지 않습니다.
                10. statistics 필드는 응답에 포함하지 않습니다.

                {
                  "workSummary": "지금까지 수행한 업무의 전체 흐름과 핵심 내용을 3~6문장으로 작성",
                  "implementedFeatures": [
                    {
                      "category": "기능 분류명",
                      "features": [
                        "구현 기능 1",
                        "구현 기능 2"
                      ],
                      "description": "해당 기능 분류에 대한 설명"
                    }
                  ],
                  "difficultyAnalysis": "어려웠던 작업과 난이도 분포를 근거로 3~5문장 작성",
                  "projectAchievements": "완성한 결과와 기술적 성과를 3~5문장 작성",
                  "futureImprovements": [
                    "향후 개선 사항 1",
                    "향후 개선 사항 2",
                    "향후 개선 사항 3"
                  ]
                }

                보고서 작성에 사용할 실제 데이터:

                %s
                """.formatted(reportData);
    }

    private String createJapaneseReportPrompt(
            String reportData
    ) {

        return """
                あなたは開発者のすべての業務ログをもとに、
                最終業務報告書を作成するAIです。

                必ず以下のルールを守ってください。

                1. すべての内容は自然な日本語で作成してください。
                2. 提供された業務ログと統計だけを使用してください。
                3. 記録に存在しない機能や成果を作らないでください。
                4. 同一または類似する業務は一つの機能としてまとめてください。
                5. 業務ログを単純に並べず、全体の流れをまとめてください。
                6. 難易度の数値と件数は入力値をそのまま解釈してください。
                7. 今後の改善事項は現在の記録をもとに現実的に作成してください。
                8. 必ず以下のJSON形式のみで回答してください。
                9. JSONの外側に説明、Markdown、コードブロックを
                   追加しないでください。
                10. statisticsフィールドは応答に含めないでください。

                {
                  "workSummary": "これまでの業務の流れと主要内容を3～6文で作成",
                  "implementedFeatures": [
                    {
                      "category": "機能分類名",
                      "features": [
                        "実装機能1",
                        "実装機能2"
                      ],
                      "description": "該当機能分類についての説明"
                    }
                  ],
                  "difficultyAnalysis": "難しかった作業と難易度分布を根拠に3～5文で作成",
                  "projectAchievements": "完成した結果と技術的成果を3～5文で作成",
                  "futureImprovements": [
                    "今後の改善事項1",
                    "今後の改善事項2",
                    "今後の改善事項3"
                  ]
                }

                報告書作成に使用する実際のデータ:

                %s
                """.formatted(reportData);
    }

    private String normalizeDifficulty(
            String difficulty
    ) {

        if (difficulty == null
                || difficulty.isBlank()) {

            return "미분류";
        }

        String value = difficulty.trim();

        if (value.contains("초급")
                || value.contains("初級")
                || value.equalsIgnoreCase("beginner")) {

            return "초급";
        }

        if (value.contains("중급")
                || value.contains("中級")
                || value.equalsIgnoreCase("intermediate")) {

            return "중급";
        }

        if (value.contains("고급")
                || value.contains("上級")
                || value.equalsIgnoreCase("advanced")) {

            return "고급";
        }

        return "미분류";
    }

    private static String removeTrailingSlash(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "http://localhost:11434";
        }

        return value.trim().replaceAll("/+$", "");
    }

    private static String safeResponseBody(
            String responseBody
    ) {

        if (responseBody == null
                || responseBody.isBlank()) {

            return "응답 내용 없음";
        }

        if (responseBody.length() <= 500) {
            return responseBody;
        }

        return responseBody.substring(0, 500)
                + "...";
    }
}