package com.example.demo.report.dto;

import java.util.ArrayList;
import java.util.List;

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
public class AiReportResponse {

    private ReportStatistics statistics;

    private String workSummary;

    @Builder.Default
    private List<ImplementedFeature> implementedFeatures =
            new ArrayList<>();

    private String difficultyAnalysis;

    private String projectAchievements;

    @Builder.Default
    private List<String> futureImprovements =
            new ArrayList<>();
}