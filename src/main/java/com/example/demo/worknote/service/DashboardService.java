package com.example.demo.worknote.service;

import com.example.demo.work.entity.WorkLog;
import com.example.demo.work.repository.WorkLogRepository;
import com.example.demo.worknote.dto.WorkLogDashboardResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WorkLogRepository workLogRepository;

    public WorkLogDashboardResponse getDashboard(Long userId) {
        List<WorkLog> workLogs =
                workLogRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Long> difficultyCounts =
                createDifficultyCounts(workLogs);

        List<WorkLogDashboardResponse.TagCount> topTags =
                createTopTags(workLogs);

        List<WorkLogDashboardResponse.DailyCount> recentDailyCounts =
                createRecentDailyCounts(workLogs);

        List<WorkLogDashboardResponse.RecentWorkLog> recentWorkLogs =
                createRecentWorkLogs(workLogs);

        return WorkLogDashboardResponse.builder()
                .totalCount(workLogs.size())
                .difficultyCounts(difficultyCounts)
                .topTags(topTags)
                .recentDailyCounts(recentDailyCounts)
                .recentWorkLogs(recentWorkLogs)
                .build();
    }

    private Map<String, Long> createDifficultyCounts(
            List<WorkLog> workLogs
    ) {
        Map<String, Long> result = new LinkedHashMap<>();

        result.put("초급", 0L);
        result.put("중급", 0L);
        result.put("고급", 0L);
        result.put("미분류", 0L);

        for (WorkLog workLog : workLogs) {
            String difficulty = normalizeDifficulty(
                    workLog.getDifficulty()
            );

            result.put(
                    difficulty,
                    result.getOrDefault(difficulty, 0L) + 1
            );
        }

        return result;
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "미분류";
        }

        String normalized = difficulty.trim();

        if (normalized.contains("초급")) {
            return "초급";
        }

        if (normalized.contains("중급")) {
            return "중급";
        }

        if (normalized.contains("고급")) {
            return "고급";
        }

        return "미분류";
    }

    private List<WorkLogDashboardResponse.TagCount> createTopTags(
            List<WorkLog> workLogs
    ) {
        Map<String, Long> tagCounts = workLogs.stream()
                .map(WorkLog::getTechTags)
                .filter(tags -> tags != null && !tags.isBlank())
                .flatMap(tags -> Arrays.stream(tags.split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(this::normalizeTag)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        return tagCounts.entrySet()
                .stream()
                .sorted(
                        Map.Entry.<String, Long>comparingByValue()
                                .reversed()
                                .thenComparing(Map.Entry::getKey)
                )
                .limit(5)
                .map(entry ->
                        WorkLogDashboardResponse.TagCount.builder()
                                .tag(entry.getKey())
                                .count(entry.getValue())
                                .build()
                )
                .toList();
    }

    private String normalizeTag(String tag) {
        String trimmed = tag.trim();

        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String lowerTag = trimmed.toLowerCase(Locale.ROOT);

        return switch (lowerTag) {
            case "spring boot", "springboot" -> "Spring Boot";
            case "react", "react.js", "reactjs" -> "React";
            case "postgresql", "postgres" -> "PostgreSQL";
            case "javascript", "js" -> "JavaScript";
            case "typescript", "ts" -> "TypeScript";
            case "docker" -> "Docker";
            case "ollama" -> "Ollama";
            case "jpa", "spring data jpa" -> "JPA";
            case "hibernate" -> "Hibernate";
            case "java" -> "Java";
            default -> trimmed;
        };
    }

    private List<WorkLogDashboardResponse.DailyCount>
    createRecentDailyCounts(List<WorkLog> workLogs) {

        LocalDate today = LocalDate.now();

        Map<LocalDate, Long> countByDate = workLogs.stream()
                .filter(workLog -> workLog.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        workLog ->
                                workLog.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MM/dd");

        return today.minusDays(6)
                .datesUntil(today.plusDays(1))
                .map(date ->
                        WorkLogDashboardResponse.DailyCount.builder()
                                .date(date.format(formatter))
                                .count(
                                        countByDate.getOrDefault(
                                                date,
                                                0L
                                        )
                                )
                                .build()
                )
                .toList();
    }

    private List<WorkLogDashboardResponse.RecentWorkLog>
    createRecentWorkLogs(List<WorkLog> workLogs) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"
                );

        return workLogs.stream()
                .limit(5)
                .map(workLog ->
                        WorkLogDashboardResponse.RecentWorkLog
                                .builder()
                                .id(workLog.getId())
                                .title(workLog.getTitle())
                                .difficulty(
                                        normalizeDifficulty(
                                                workLog.getDifficulty()
                                        )
                                )
                                .createdAt(
                                        workLog.getCreatedAt() == null
                                                ? ""
                                                : workLog.getCreatedAt()
                                                .format(formatter)
                                )
                                .build()
                )
                .toList();
    }
}