package com.example.demo.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ai.dto.AiAnalysisResponse;
import com.example.demo.ai.service.OllamaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final OllamaService ollamaService;

    /**
     * Ollama 연결 테스트 API
     *
     * 한국어 테스트:
     * GET /api/ai/test
     *
     * 일본어 테스트:
     * GET /api/ai/test?language=ja
     */
    @GetMapping("/api/ai/test")
    public ResponseEntity<AiAnalysisResponse> test(
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

        AiAnalysisResponse response =
                ollamaService.analyze(
                        sample,
                        language
                );

        return ResponseEntity.ok(response);
    }
}