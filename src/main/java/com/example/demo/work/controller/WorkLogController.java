package com.example.demo.work.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.OpenApiConfig;
import com.example.demo.work.dto.WorkLogRequest;
import com.example.demo.work.response.WorkLogResponse;
import com.example.demo.work.service.WorkLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
@Tag(name = "업무일지", description = "업무일지 생성, 조회, 수정, 삭제 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class WorkLogController {

    private final WorkLogService workLogService;

    @PostMapping
    @Operation(
            summary = "업무일지 생성",
            description = "업무 내용을 저장하고 AI 분석 결과를 함께 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무일지 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "429", description = "일일 AI 사용량 초과")
    })
    public ResponseEntity<Void> save(
            @RequestBody WorkLogRequest request
    ) {
        workLogService.save(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "사용자 업무일지 전체 조회",
            description = "사용자 ID에 해당하는 업무일지와 AI 분석 결과를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<WorkLogResponse>> findAll(
            @Parameter(description = "조회할 사용자 ID", example = "1")
            @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(
                workLogService.findAll(userId)
        );
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "업무일지 수정",
            description = "업무일지 내용을 수정하고 AI 분석 결과를 다시 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "업무일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "429", description = "일일 AI 사용량 초과")
    })
    public ResponseEntity<Void> update(
            @Parameter(description = "수정할 업무일지 ID", example = "1")
            @PathVariable("id") Long id,
            @RequestBody WorkLogRequest request
    ) {
        workLogService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "업무일지 삭제",
            description = "지정한 업무일지를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "업무일지를 찾을 수 없음")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 업무일지 ID", example = "1")
            @PathVariable("id") Long id
    ) {
        workLogService.delete(id);
        return ResponseEntity.ok().build();
    }
}
