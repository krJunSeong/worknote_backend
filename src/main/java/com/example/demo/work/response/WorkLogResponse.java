package com.example.demo.work.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class WorkLogResponse {

    private Long id;

    private String title;

    private String content;

    private String aiSummary;

    private String techTags;

    private String interviewQuestions;

    private String difficulty;

    private LocalDateTime createdAt;

    private LocalDate workDate;
}
