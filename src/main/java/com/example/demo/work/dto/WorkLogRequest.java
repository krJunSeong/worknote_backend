package com.example.demo.work.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogRequest {

    private Long userId;

    private String title;

    private String content;

    private String language;

    private LocalDate workDate;
}
