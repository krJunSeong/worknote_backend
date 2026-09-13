package com.example.demo.worknote.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.worknote.dto.WorkLogDashboardResponse;
import com.example.demo.worknote.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Current endpoint. The client does not provide a user id; the server
     * resolves the owner from the authenticated JWT.
     */
    @GetMapping({"", "/"})
    public ResponseEntity<WorkLogDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    /**
     * Backward-compatible endpoint for older frontends.
     * The requested id is never trusted: it must match the JWT owner.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WorkLogDashboardResponse> getDashboardLegacy(
            @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(
                dashboardService.getDashboardForUser(userId)
        );
    }
}
