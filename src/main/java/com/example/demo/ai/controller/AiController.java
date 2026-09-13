package com.example.demo.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ai.dto.AiAnalysisResponse;
import com.example.demo.ai.service.OllamaService;
import com.example.demo.config.OpenApiConfig;
import com.example.demo.usage.service.DailyUsageLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI", description = "Ollama AI 연결 및 분석 테스트 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AiController {

    private final OllamaService ollamaService;
    private final DailyUsageLimitService dailyUsageLimitService;

    @GetMapping("/api/ai/test")
    @Operation(
            summary = "AI 분석 연결 테스트",
            description = "샘플 업무 내용을 Ollama에 전달해 AI 요약, 기술 태그, 난이도, 면접 질문을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 분석 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "429", description = "일일 AI 사용량 초과"),
            @ApiResponse(responseCode = "500", description = "Ollama 연결 또는 분석 실패")
    })
    public ResponseEntity<AiAnalysisResponse> test(
            @Parameter(
                    description = "분석 결과 언어",
                    example = "ko"
            )
            @RequestParam(
                    name = "language",
                    defaultValue = "ko"
            )
            String language
    ) {
        String sample = """
                제목:
                Spring Boot와 React 연동 작업

                업무 내용:
                오늘 Spring Boot와 React를 연결했습니다.
                업무일지 CRUD 기능을 구현했으며,
                Docker PostgreSQL 데이터베이스를 연결했습니다.
                이후 Ollama를 이용한 AI 분석 기능 연동을 시작했습니다.
                """;

        dailyUsageLimitService.consumeAi();

        AiAnalysisResponse response =
                ollamaService.analyze(
                        sample,
                        language
                );

        return ResponseEntity.ok(response);
    }
}
