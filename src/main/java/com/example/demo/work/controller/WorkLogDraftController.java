package com.example.demo.work.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.exception.DailyUsageLimitExceededException;
import com.example.demo.config.OpenApiConfig;
import com.example.demo.work.response.WorkLogDraftResponse;
import com.example.demo.work.service.WorkLogDraftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/work/draft")
@Tag(
        name = "업무일지 초안",
        description = "메모 이미지 OCR 및 AI 업무일지 초안 생성 API"
)
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class WorkLogDraftController {

    private final WorkLogDraftService workLogDraftService;

    public WorkLogDraftController(
            WorkLogDraftService workLogDraftService
    ) {
        this.workLogDraftService = workLogDraftService;
    }

    @PostMapping(
            value = "/from-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "메모 이미지에서 업무일지 초안 생성",
            description = "이미지의 글자를 OCR로 추출한 뒤 AI가 제목과 업무내용 초안을 생성합니다. DB에는 저장하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초안 생성 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 또는 요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "429", description = "일일 AI/OCR 사용량 초과"),
            @ApiResponse(responseCode = "502", description = "OCR 또는 AI 처리 실패"),
            @ApiResponse(responseCode = "503", description = "OCR 설정 또는 서비스 사용 불가")
    })
    public ResponseEntity<WorkLogDraftResponse> createFromImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        return ResponseEntity.ok(
                workLogDraftService.createFromImage(
                        image,
                        language
                )
        );
    }

    @ExceptionHandler(DailyUsageLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleDailyLimit(
            DailyUsageLimitExceededException e
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                Map.of(
                        "code", DailyUsageLimitExceededException.CODE,
                        "message", DailyUsageLimitExceededException.DEFAULT_MESSAGE
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException e
    ) {
        return ResponseEntity.badRequest().body(
                Map.of("message", safeMessage(
                        e,
                        "메모 이미지 요청을 확인해 주세요."
                ))
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(
            IllegalStateException e
    ) {
        return ResponseEntity.status(
                HttpStatus.SERVICE_UNAVAILABLE
        ).body(
                Map.of("message", safeMessage(
                        e,
                        "OCR 기능을 사용할 수 없습니다."
                ))
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleProcessingError(
            RuntimeException e
    ) {
        return ResponseEntity.status(
                HttpStatus.BAD_GATEWAY
        ).body(
                Map.of("message", safeMessage(
                        e,
                        "OCR 또는 AI 처리에 실패했습니다."
                ))
        );
    }

    private String safeMessage(
            Exception e,
            String fallback
    ) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? fallback
                : message;
    }
}
