package com.example.demo.goal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.demo.goal.model.GoalStatus;

public class GoalResponse {
    private final Long id;
    private final String title;
    private final String description;
    private final LocalDate targetDate;
    private final GoalStatus status;
    private final Integer progress;
    private final boolean overdue;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public GoalResponse(
            Long id,
            String title,
            String description,
            LocalDate targetDate,
            GoalStatus status,
            Integer progress,
            boolean overdue,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = status;
        this.progress = progress;
        this.overdue = overdue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }
    public Integer getProgress() { return progress; }
    public boolean isOverdue() { return overdue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
