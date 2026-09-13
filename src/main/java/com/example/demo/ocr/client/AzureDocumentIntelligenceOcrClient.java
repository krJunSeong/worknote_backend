package com.example.demo.ocr.client;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AzureDocumentIntelligenceOcrClient implements OcrClient {

    private static final String SUBSCRIPTION_KEY_HEADER =
            "Ocp-Apim-Subscription-Key";

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String endpoint;
    private final String apiKey;
    private final String apiVersion;
    private final String modelId;
    private final Duration timeout;
    private final long pollIntervalMillis;

    public AzureDocumentIntelligenceOcrClient(
            ObjectMapper objectMapper,
            @Value("${ocr.azure.endpoint:}") String endpoint,
            @Value("${ocr.azure.api-key:}") String apiKey,
            @Value("${ocr.azure.api-version:2024-11-30}") String apiVersion,
            @Value("${ocr.azure.model-id:prebuilt-read}") String modelId,
            @Value("${ocr.azure.timeout-seconds:60}") long timeoutSeconds,
            @Value("${ocr.azure.poll-interval-millis:1000}") long pollIntervalMillis
    ) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
        this.endpoint = removeTrailingSlash(endpoint);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiVersion = apiVersion == null || apiVersion.isBlank()
                ? "2024-11-30"
                : apiVersion.trim();
        this.modelId = modelId == null || modelId.isBlank()
                ? "prebuilt-read"
                : modelId.trim();
        this.timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 1L));
        this.pollIntervalMillis = Math.max(pollIntervalMillis, 250L);
    }

    @Override
    public String extractText(MultipartFile image) {
        validateConfiguration();
        validateContentType(image);

        String operationLocation = startAnalysis(image);
        return pollResult(operationLocation);
    }

    private String startAnalysis(MultipartFile image) {
        String analyzeUrl = endpoint
                + "/documentintelligence/documentModels/"
                + modelId
                + ":analyze?api-version="
                + apiVersion;

        try {
            MediaType contentType = resolveContentType(image);

            ResponseEntity<Void> response = webClient.post()
                    .uri(analyzeUrl)
                    .header(SUBSCRIPTION_KEY_HEADER, apiKey)
                    .contentType(contentType)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(image.getBytes())
                    .retrieve()
                    .toBodilessEntity()
                    .block(timeout);

            if (response == null) {
                throw new RuntimeException(
                        "OCR 서버에서 분석 시작 응답을 받지 못했습니다."
                );
            }

            String operationLocation = response.getHeaders()
                    .getFirst("Operation-Location");

            if (operationLocation == null || operationLocation.isBlank()) {
                throw new RuntimeException(
                        "OCR 서버 응답에 Operation-Location이 없습니다."
                );
            }

            return operationLocation;

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "OCR 서버 요청에 실패했습니다. 상태 코드: "
                            + e.getStatusCode().value()
                            + ", 응답: "
                            + safeResponseBody(e.getResponseBodyAsString()),
                    e
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "OCR 서버에 이미지를 전송하지 못했습니다.",
                    e
            );
        }
    }

    private String pollResult(String operationLocation) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadlineNanos) {
            Map<String, Object> response;

            try {
                response = webClient.get()
                        .uri(operationLocation)
                        .header(SUBSCRIPTION_KEY_HEADER, apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(
                                new ParameterizedTypeReference<
                                        Map<String, Object>
                                >() {
                                }
                        )
                        .block(remainingTimeout(deadlineNanos));

            } catch (WebClientResponseException e) {
                throw new RuntimeException(
                        "OCR 결과 조회에 실패했습니다. 상태 코드: "
                                + e.getStatusCode().value()
                                + ", 응답: "
                                + safeResponseBody(e.getResponseBodyAsString()),
                        e
                );
            }

            if (response == null) {
                throw new RuntimeException(
                        "OCR 결과 응답이 없습니다."
                );
            }

            JsonNode root = objectMapper.valueToTree(response);
            String status = root.path("status").asText("");

            if ("succeeded".equalsIgnoreCase(status)) {
                String content = root.path("analyzeResult")
                        .path("content")
                        .asText("")
                        .trim();

                if (content.isBlank()) {
                    content = extractFallbackContent(root);
                }

                return content.trim();
            }

            if ("failed".equalsIgnoreCase(status)) {
                String message = root.path("error")
                        .path("message")
                        .asText("OCR 분석에 실패했습니다.");

                throw new RuntimeException(
                        "OCR 분석에 실패했습니다: " + message
                );
            }

            sleepBeforeNextPoll();
        }

        throw new RuntimeException(
                "OCR 분석 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
        );
    }

    private String extractFallbackContent(JsonNode root) {
        StringBuilder text = new StringBuilder();
        JsonNode pages = root.path("analyzeResult").path("pages");

        if (!pages.isArray()) {
            return "";
        }

        for (JsonNode page : pages) {
            JsonNode lines = page.path("lines");

            if (!lines.isArray()) {
                continue;
            }

            for (JsonNode line : lines) {
                String content = line.path("content").asText("").trim();

                if (!content.isBlank()) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(content);
                }
            }
        }

        return text.toString();
    }

    private void validateConfiguration() {
        if (endpoint.isBlank() || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OCR 기능이 설정되지 않았습니다. "
                            + "OCR_AZURE_ENDPOINT와 OCR_AZURE_API_KEY 환경변수를 확인해 주세요."
            );
        }
    }

    private void validateContentType(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "분석할 이미지가 없습니다."
            );
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "OCR은 JPG/JPEG/PNG 이미지만 지원합니다."
            );
        }
    }

    private MediaType resolveContentType(MultipartFile image) {
        try {
            return MediaType.parseMediaType(image.getContentType());
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private Duration remainingTimeout(long deadlineNanos) {
        long remainingNanos = Math.max(
                deadlineNanos - System.nanoTime(),
                Duration.ofMillis(250).toNanos()
        );
        return Duration.ofNanos(remainingNanos);
    }

    private void sleepBeforeNextPoll() {
        try {
            Thread.sleep(pollIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "OCR 결과 대기가 중단되었습니다.",
                    e
            );
        }
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String safeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "응답 내용 없음";
        }

        String normalized = responseBody.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500
                ? normalized
                : normalized.substring(0, 500) + "...";
    }
}
