package com.example.demo.goal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.goal.dto.GoalRequest;
import com.example.demo.goal.dto.GoalResponse;
import com.example.demo.goal.service.GoalService;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> findAll() {
        return ResponseEntity.ok(goalService.findAll());
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable("id") Long id, @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
