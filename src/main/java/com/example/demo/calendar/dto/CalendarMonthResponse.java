package com.example.demo.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.goal.model.GoalStatus;

public class CalendarMonthResponse {
    private final int year;
    private final int month;
    private final List<WorkLogItem> workLogs;
    private final List<GoalItem> goals;

    public CalendarMonthResponse(int year, int month, List<WorkLogItem> workLogs, List<GoalItem> goals) {
        this.year = year;
        this.month = month;
        this.workLogs = workLogs;
        this.goals = goals;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public List<WorkLogItem> getWorkLogs() { return workLogs; }
    public List<GoalItem> getGoals() { return goals; }

    public static class WorkLogItem {
        private final Long id;
        private final String title;
        private final LocalDateTime createdAt;

        public WorkLogItem(Long id, String title, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class GoalItem {
        private final Long id;
        private final String title;
        private final LocalDate targetDate;
        private final GoalStatus status;
        private final Integer progress;
        private final boolean overdue;

        public GoalItem(Long id, String title, LocalDate targetDate, GoalStatus status, Integer progress, boolean overdue) {
            this.id = id;
            this.title = title;
            this.targetDate = targetDate;
            this.status = status;
            this.progress = progress;
            this.overdue = overdue;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public LocalDate getTargetDate() { return targetDate; }
        public GoalStatus getStatus() { return status; }
        public Integer getProgress() { return progress; }
        public boolean isOverdue() { return overdue; }
    }
}
