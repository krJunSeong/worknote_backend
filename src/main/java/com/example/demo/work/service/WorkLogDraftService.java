package com.example.demo.work.service;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.service.OllamaService;
import com.example.demo.ocr.client.OcrClient;
import com.example.demo.usage.service.DailyUsageLimitService;
import com.example.demo.work.response.WorkLogDraftResponse;

@Service
public class WorkLogDraftService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final int MIN_IMAGE_DIMENSION = 50;
    private static final int MAX_IMAGE_DIMENSION = 10_000;
    private static final int MAX_DRAFT_CONTENT_LENGTH = 20_000;

    private final OcrClient ocrClient;
    private final OllamaService ollamaService;
    private final DailyUsageLimitService dailyUsageLimitService;

    public WorkLogDraftService(
            OcrClient ocrClient,
            OllamaService ollamaService,
            DailyUsageLimitService dailyUsageLimitService
    ) {
        this.ocrClient = ocrClient;
        this.ollamaService = ollamaService;
        this.dailyUsageLimitService = dailyUsageLimitService;
    }

    @Value("${ocr.max-file-size-bytes:4194304}")
    private long maxFileSizeBytes;

    @Value("${ocr.max-recognized-text-chars:20000}")
    private int maxRecognizedTextChars;

    public WorkLogDraftResponse createFromImage(
            MultipartFile image,
            String language
    ) {
        validateImage(image);

        // 이미지 초안 생성은 Azure OCR + AI를 모두 사용한다.
        // 외부 서비스를 호출하기 전에 두 일일 한도를 함께 확인/차감한다.
        dailyUsageLimitService.consumeImageDraft();

        String normalizedLanguage = normalizeLanguage(language);
        String recognizedText = ocrClient.extractText(image).trim();

        if (recognizedText.isBlank()) {
            throw new IllegalArgumentException(
                    "이미지에서 인식된 텍스트가 없습니다. "
                            + "메모가 더 선명하게 보이도록 촬영해 주세요."
            );
        }

        if (recognizedText.length() > maxRecognizedTextChars) {
            throw new IllegalArgumentException(
                    "한 번에 인식된 텍스트가 너무 많습니다. "
                            + "메모 이미지를 나누어 업로드해 주세요."
            );
        }

        WorkLogDraftResponse draft =
                ollamaService.generateWorkLogDraft(
                        recognizedText,
                        normalizedLanguage
                );

        String title = safeText(draft.getTitle());
        String content = safeText(draft.getContent());

        if (title.isBlank()) {
            title = "ja".equals(normalizedLanguage)
                    ? "メモ画像から作成した業務日誌"
                    : "메모 사진에서 작성한 업무일지";
        }

        if (content.isBlank()) {
            content = recognizedText;
        }

        title = truncate(title, 200);
        content = truncate(content, MAX_DRAFT_CONTENT_LENGTH);

        return new WorkLogDraftResponse(
                title,
                content,
                recognizedText
        );
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "메모 이미지를 선택해 주세요."
            );
        }

        if (image.getSize() > maxFileSizeBytes) {
            long maxMb = Math.max(1L, maxFileSizeBytes / 1024 / 1024);
            throw new IllegalArgumentException(
                    "이미지 용량은 " + maxMb + "MB 이하로 업로드해 주세요."
            );
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "JPG/JPEG/PNG 이미지 파일만 업로드할 수 있습니다."
            );
        }

        try {
            byte[] bytes = image.getBytes();

            try (ImageInputStream input = ImageIO.createImageInputStream(
                    new ByteArrayInputStream(bytes)
            )) {
                if (input == null) {
                    throw new IllegalArgumentException(
                            "올바른 이미지 파일이 아닙니다."
                    );
                }

                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new IllegalArgumentException(
                            "올바른 이미지 파일이 아닙니다."
                    );
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);

                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    String formatName = reader.getFormatName();

                    if (!("JPEG".equalsIgnoreCase(formatName)
                            || "JPG".equalsIgnoreCase(formatName)
                            || "PNG".equalsIgnoreCase(formatName))) {
                        throw new IllegalArgumentException(
                                "JPG/JPEG/PNG 이미지 파일만 업로드할 수 있습니다."
                        );
                    }

                    if (width < MIN_IMAGE_DIMENSION
                            || height < MIN_IMAGE_DIMENSION
                            || width > MAX_IMAGE_DIMENSION
                            || height > MAX_IMAGE_DIMENSION) {

                        throw new IllegalArgumentException(
                                "이미지 크기는 50x50 이상, 10000x10000 이하이어야 합니다."
                        );
                    }
                } finally {
                    reader.dispose();
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "이미지 파일을 확인하지 못했습니다.",
                    e
            );
        }
    }

    private String normalizeLanguage(String language) {
        return "ja".equalsIgnoreCase(language)
                ? "ja"
                : "ko";
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }
}
