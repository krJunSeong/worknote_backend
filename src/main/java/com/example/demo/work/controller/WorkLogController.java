package com.example.demo.work.controller;

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

import com.example.demo.work.dto.WorkLogRequest;
import com.example.demo.work.response.WorkLogResponse;
import com.example.demo.work.service.WorkLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    @PostMapping
    public ResponseEntity<Void> save(
            @RequestBody WorkLogRequest request
    ) {

        workLogService.save(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<WorkLogResponse>> findAll(
            @PathVariable("userId") Long userId
    ) {

        return ResponseEntity.ok(
                workLogService.findAll(userId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable("id") Long id,
            @RequestBody WorkLogRequest request
    ) {

        workLogService.update(id, request);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id
    ) {

        workLogService.delete(id);

        return ResponseEntity.ok().build();
    }
}