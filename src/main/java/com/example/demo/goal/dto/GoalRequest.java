package com.example.demo.goal.dto;

import java.time.LocalDate;
import com.example.demo.goal.model.GoalStatus;

public class GoalRequest {
    private String title;
    private String description;
    private LocalDate targetDate;
    private GoalStatus status;
    private Integer progress;

    public GoalRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
}
