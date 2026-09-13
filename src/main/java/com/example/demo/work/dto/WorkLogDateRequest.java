package com.example.demo.work.dto;

import java.time.LocalDate;

public class WorkLogDateRequest {
    private LocalDate workDate;

    public WorkLogDateRequest() {
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }
}
