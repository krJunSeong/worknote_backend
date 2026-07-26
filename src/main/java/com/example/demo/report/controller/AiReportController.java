package com.example.demo.report.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.report.dto.AiReportResponse;
import com.example.demo.report.service.AiReportService;
import com.example.demo.report.service.PdfReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;
    private final PdfReportService pdfReportService;

    @PostMapping("/ai")
    public ResponseEntity<AiReportResponse>
    generateAiReport(
            @RequestParam(
                    name = "language",
                    defaultValue = "ko"
            )
            String language
    ) {

        AiReportResponse response =
                aiReportService.generateReport(
                        language
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/ai/pdf",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> downloadPdf(
            @RequestBody
            AiReportResponse report
    ) {

        byte[] pdf =
                pdfReportService.createPdf(
                        report
                );

        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                "AI_최종_업무_보고서.pdf",
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(pdf.length)
                .body(pdf);
    }
}