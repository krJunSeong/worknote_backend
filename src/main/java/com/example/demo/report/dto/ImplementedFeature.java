package com.example.demo.report.dto;

import java.util.ArrayList;
import java.util.List;

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
public class ImplementedFeature {

    private String category;

    @Builder.Default
    private List<String> features =
            new ArrayList<>();

    private String description;
}