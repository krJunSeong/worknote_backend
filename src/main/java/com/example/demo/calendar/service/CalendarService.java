package com.example.demo.calendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.calendar.dto.CalendarMonthResponse;
import com.example.demo.goal.entity.Goal;
import com.example.demo.goal.model.GoalStatus;
import com.example.demo.goal.repository.GoalRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.work.entity.WorkLog;
import com.example.demo.work.repository.WorkLogRepository;

@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final WorkLogRepository workLogRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public CalendarService(
            WorkLogRepository workLogRepository,
            GoalRepository goalRepository,
            UserRepository userRepository
    ) {
        this.workLogRepository = workLogRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public CalendarMonthResponse getMonth(int year, int month) {
        YearMonth yearMonth = validateYearMonth(year, month);
        User loginUser = getLoginUser();

        LocalDate firstDate = yearMonth.atDay(1);
        LocalDate lastDate = yearMonth.atEndOfMonth();
        LocalDateTime startDateTime = firstDate.atStartOfDay();
        LocalDateTime endDateTime = lastDate.plusDays(1).atStartOfDay();

        List<CalendarMonthResponse.WorkLogItem> workLogs = workLogRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        loginUser.getId(), startDateTime, endDateTime
                )
                .stream()
                .map(this::toWorkLogItem)
                .toList();

        List<CalendarMonthResponse.GoalItem> goals = goalRepository
                .findByUserIdAndTargetDateBetweenOrderByTargetDateAscCreatedAtAsc(
                        loginUser.getId(), firstDate, lastDate
                )
                .stream()
                .map(this::toGoalItem)
                .toList();

        return new CalendarMonthResponse(year, month, workLogs, goals);
    }

    private YearMonth validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("조회 연도는 2000~2100 사이여야 합니다.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("조회 월은 1~12 사이여야 합니다.");
        }
        return YearMonth.of(year, month);
    }

    private CalendarMonthResponse.WorkLogItem toWorkLogItem(WorkLog workLog) {
        return new CalendarMonthResponse.WorkLogItem(workLog.getId(), workLog.getTitle(), workLog.getCreatedAt());
    }

    private CalendarMonthResponse.GoalItem toGoalItem(Goal goal) {
        boolean overdue = goal.getTargetDate() != null
                && goal.getTargetDate().isBefore(LocalDate.now())
                && goal.getStatus() != GoalStatus.COMPLETED;
        return new CalendarMonthResponse.GoalItem(
                goal.getId(), goal.getTitle(), goal.getTargetDate(), goal.getStatus(), goal.getProgress(), overdue
        );
    }

    private User getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        return userRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("로그인 사용자를 찾을 수 없습니다."));
    }
}
