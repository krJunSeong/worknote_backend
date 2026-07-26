package com.example.demo.ai.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    private String summary;
    private List<String> techTags;
    private List<String> questions;
    private String difficulty;
}