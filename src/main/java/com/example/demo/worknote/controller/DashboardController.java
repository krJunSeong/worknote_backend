package com.example.demo.worknote.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.worknote.dto.WorkLogDashboardResponse;
import com.example.demo.worknote.service.DashboardService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{userId}")
    public ResponseEntity<WorkLogDashboardResponse> getDashboard(
            @PathVariable("userId") Long userId
    ) {
        WorkLogDashboardResponse response =
                dashboardService.getDashboard(userId);

        return ResponseEntity.ok(response);
    }
}