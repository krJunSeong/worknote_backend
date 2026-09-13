package com.example.demo.calendar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.calendar.dto.CalendarMonthResponse;
import com.example.demo.calendar.service.CalendarService;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public ResponseEntity<CalendarMonthResponse> getMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        return ResponseEntity.ok(calendarService.getMonth(year, month));
    }
}
