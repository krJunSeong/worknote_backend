package com.example.demo.report.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.service.OllamaService;
import com.example.demo.report.dto.AiReportResponse;
import com.example.demo.report.dto.ImplementedFeature;
import com.example.demo.report.dto.ReportStatistics;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.work.entity.WorkLog;
import com.example.demo.work.repository.WorkLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd"
            );

    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;

    public AiReportResponse generateReport(
            String language
    ) {

        User loginUser = getLoginUser();

        List<WorkLog> workLogs =
                workLogRepository
                        .findByUserIdOrderByCreatedAtAsc(
                                loginUser.getId()
                        );

        if (workLogs.isEmpty()) {
            throw new IllegalStateException(
                    "보고서를 생성할 업무 로그가 없습니다."
            );
        }

        ReportStatistics statistics =
                createStatistics(workLogs);

        String reportInput =
                createReportInput(
                        loginUser,
                        workLogs,
                        statistics,
                        normalizeLanguage(language)
                );

        AiReportResponse report =
                ollamaService.generateReport(
                        reportInput,
                        normalizeLanguage(language)
                );

        normalizeReport(report);

        report.setStatistics(statistics);

        return report;
    }

    private User getLoginUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new RuntimeException(
                    "로그인이 필요합니다."
            );
        }

        return userRepository
                .findByLoginId(
                        authentication.getName()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "로그인 사용자를 찾을 수 없습니다."
                        )
                );
    }

    private ReportStatistics createStatistics(
            List<WorkLog> workLogs
    ) {

        LocalDate startDate =
                workLogs.getFirst()
                        .getCreatedAt()
                        .toLocalDate();

        LocalDate endDate =
                workLogs.getLast()
                        .getCreatedAt()
                        .toLocalDate();

        Map<String, Long> difficultyCounts =
                createDifficultyCounts(workLogs);

        Map<String, Long> tagCounts =
                createTagCounts(workLogs);

        double averageDifficulty =
                workLogs.stream()
                        .mapToInt(workLog ->
                                convertDifficultyToScore(
                                        workLog.getDifficulty()
                                )
                        )
                        .filter(score -> score > 0)
                        .average()
                        .orElse(0.0);

        averageDifficulty =
                Math.round(
                        averageDifficulty * 10.0
                ) / 10.0;

        return ReportStatistics.builder()
                .totalWorkLogs(workLogs.size())
                .startDate(startDate)
                .endDate(endDate)
                .averageDifficulty(averageDifficulty)
                .difficultyCounts(difficultyCounts)
                .tagCounts(tagCounts)
                .build();
    }

    private Map<String, Long> createDifficultyCounts(
            List<WorkLog> workLogs
    ) {

        Map<String, Long> counts =
                new LinkedHashMap<>();

        counts.put("초급", 0L);
        counts.put("중급", 0L);
        counts.put("고급", 0L);
        counts.put("미분류", 0L);

        for (WorkLog workLog : workLogs) {
            String difficulty =
                    normalizeDifficulty(
                            workLog.getDifficulty()
                    );

            counts.put(
                    difficulty,
                    counts.getOrDefault(
                            difficulty,
                            0L
                    ) + 1L
            );
        }

        return counts;
    }

    private Map<String, Long> createTagCounts(
            List<WorkLog> workLogs
    ) {

        Map<String, Long> counts =
                workLogs.stream()
                        .flatMap(workLog ->
                                splitTags(
                                        workLog.getTechTags()
                                ).stream()
                        )
                        .collect(
                                Collectors.groupingBy(
                                        tag -> tag,
                                        Collectors.counting()
                                )
                        );

        return counts.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Long>comparingByValue()
                                .reversed()
                                .thenComparing(
                                        Map.Entry.comparingByKey()
                                )
                )
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (first, second) -> first,
                                LinkedHashMap::new
                        )
                );
    }

    private List<String> splitTags(
            String techTags
    ) {

        if (techTags == null
                || techTags.isBlank()) {

            return List.of();
        }

        return Arrays.stream(
                        techTags.split(",")
                )
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private int convertDifficultyToScore(
            String difficulty
    ) {

        return switch (
                normalizeDifficulty(difficulty)
        ) {
            case "초급" -> 1;
            case "중급" -> 2;
            case "고급" -> 3;
            default -> 0;
        };
    }

    private String normalizeDifficulty(
            String difficulty
    ) {

        if (difficulty == null
                || difficulty.isBlank()) {

            return "미분류";
        }

        String value =
                difficulty.trim()
                        .toLowerCase(Locale.ROOT);

        if (value.contains("초급")
                || value.contains("初級")
                || value.contains("beginner")) {

            return "초급";
        }

        if (value.contains("중급")
                || value.contains("中級")
                || value.contains("intermediate")) {

            return "중급";
        }

        if (value.contains("고급")
                || value.contains("上級")
                || value.contains("advanced")) {

            return "고급";
        }

        return "미분류";
    }

    private String createReportInput(
            User user,
            List<WorkLog> workLogs,
            ReportStatistics statistics,
            String language
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("사용자 정보\n");
        builder.append("- 사용자 ID: ")
                .append(user.getId())
                .append("\n\n");

        builder.append("전체 통계\n");
        builder.append("- 총 업무 로그 수: ")
                .append(statistics.getTotalWorkLogs())
                .append("\n");

        builder.append("- 업무 시작일: ")
                .append(formatDate(
                        statistics.getStartDate()
                ))
                .append("\n");

        builder.append("- 업무 종료일: ")
                .append(formatDate(
                        statistics.getEndDate()
                ))
                .append("\n");

        builder.append("- 평균 난이도 점수: ")
                .append(
                        statistics.getAverageDifficulty()
                )
                .append(" / 3.0\n");

        builder.append("- 난이도별 개수: ")
                .append(
                        statistics.getDifficultyCounts()
                )
                .append("\n");

        builder.append("- 기술 태그별 개수: ")
                .append(
                        statistics.getTagCounts()
                )
                .append("\n\n");

        builder.append("전체 업무 로그\n\n");

        for (int index = 0;
             index < workLogs.size();
             index++) {

            WorkLog workLog =
                    workLogs.get(index);

            builder.append("[업무 로그 ")
                    .append(index + 1)
                    .append("]\n");

            builder.append("작성일: ")
                    .append(
                            workLog.getCreatedAt()
                                    .format(
                                            DateTimeFormatter
                                                    .ofPattern(
                                                            "yyyy-MM-dd HH:mm"
                                                    )
                                    )
                    )
                    .append("\n");

            builder.append("제목: ")
                    .append(
                            getSafeText(
                                    workLog.getTitle()
                            )
                    )
                    .append("\n");

            builder.append("업무 내용:\n")
                    .append(
                            getSafeText(
                                    workLog.getContent()
                            )
                    )
                    .append("\n");

            builder.append("기존 AI 요약: ")
                    .append(
                            getSafeText(
                                    workLog.getAiSummary()
                            )
                    )
                    .append("\n");

            builder.append("기술 태그: ")
                    .append(
                            getSafeText(
                                    workLog.getTechTags()
                            )
                    )
                    .append("\n");

            builder.append("난이도: ")
                    .append(
                            normalizeDifficulty(
                                    workLog.getDifficulty()
                            )
                    )
                    .append("\n\n");
        }

        builder.append("보고서 언어: ")
                .append(language)
                .append("\n");

        return builder.toString();
    }

    private void normalizeReport(
            AiReportResponse report
    ) {

        if (report == null) {
            throw new RuntimeException(
                    "AI 보고서 결과가 없습니다."
            );
        }

        report.setWorkSummary(
                getSafeText(
                        report.getWorkSummary()
                )
        );

        report.setDifficultyAnalysis(
                getSafeText(
                        report.getDifficultyAnalysis()
                )
        );

        report.setProjectAchievements(
                getSafeText(
                        report.getProjectAchievements()
                )
        );

        if (report.getImplementedFeatures() == null) {
            report.setImplementedFeatures(
                    List.of()
            );
        } else {
            List<ImplementedFeature> features =
                    report.getImplementedFeatures()
                            .stream()
                            .filter(feature ->
                                    feature != null
                            )
                            .peek(feature -> {
                                feature.setCategory(
                                        getSafeText(
                                                feature.getCategory()
                                        )
                                );

                                feature.setDescription(
                                        getSafeText(
                                                feature.getDescription()
                                        )
                                );

                                if (feature.getFeatures() == null) {
                                    feature.setFeatures(
                                            List.of()
                                    );
                                } else {
                                    feature.setFeatures(
                                            feature.getFeatures()
                                                    .stream()
                                                    .filter(value ->
                                                            value != null
                                                    )
                                                    .map(String::trim)
                                                    .filter(value ->
                                                            !value.isBlank()
                                                    )
                                                    .distinct()
                                                    .toList()
                                    );
                                }
                            })
                            .toList();

            report.setImplementedFeatures(
                    features
            );
        }

        if (report.getFutureImprovements() == null) {
            report.setFutureImprovements(
                    List.of()
            );
        } else {
            report.setFutureImprovements(
                    report.getFutureImprovements()
                            .stream()
                            .filter(value ->
                                    value != null
                            )
                            .map(String::trim)
                            .filter(value ->
                                    !value.isBlank()
                            )
                            .distinct()
                            .toList()
            );
        }
    }

    private String normalizeLanguage(
            String language
    ) {

        if ("ja".equalsIgnoreCase(language)) {
            return "ja";
        }

        return "ko";
    }

    private String getSafeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String formatDate(
            LocalDate date
    ) {

        if (date == null) {
            return "-";
        }

        return date.format(
                DATE_FORMATTER
        );
    }
}