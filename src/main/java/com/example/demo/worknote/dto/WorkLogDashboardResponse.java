package com.example.demo.worknote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogDashboardResponse {

    private long totalCount;

    private Map<String, Long> difficultyCounts;

    private List<TagCount> topTags;

    private List<DailyCount> recentDailyCounts;

    private List<RecentWorkLog> recentWorkLogs;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagCount {
        private String tag;
        private long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private String date;
        private long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentWorkLog {
        private Long id;
        private String title;
        private String difficulty;
        private String createdAt;
    }
}