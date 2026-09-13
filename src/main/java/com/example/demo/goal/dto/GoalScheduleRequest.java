package com.example.demo.goal.dto;

import java.time.LocalDate;

public class GoalScheduleRequest {
    private LocalDate startDate;
    private LocalDate targetDate;

    public GoalScheduleRequest() {
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
}
