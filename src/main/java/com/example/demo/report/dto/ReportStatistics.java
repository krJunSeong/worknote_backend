package com.example.demo.report.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatistics {

    private int totalWorkLogs;

    private LocalDate startDate;

    private LocalDate endDate;

    private double averageDifficulty;

    @Builder.Default
    private Map<String, Long> difficultyCounts =
            new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Long> tagCounts =
            new LinkedHashMap<>();
}