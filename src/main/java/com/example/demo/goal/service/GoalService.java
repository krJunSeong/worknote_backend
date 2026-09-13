package com.example.demo.goal.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.goal.dto.GoalProgressRequest;
import com.example.demo.goal.dto.GoalRequest;
import com.example.demo.goal.dto.GoalResponse;
import com.example.demo.goal.dto.GoalScheduleRequest;
import com.example.demo.goal.entity.Goal;
import com.example.demo.goal.model.GoalStatus;
import com.example.demo.goal.repository.GoalRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class GoalService {

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 3_000;
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final long MAX_PLAN_DAYS = 3650;

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public List<GoalResponse> findAll() {
        User loginUser = getLoginUser();
        return goalRepository.findByUserIdOrderByTargetDateAscCreatedAtDesc(loginUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        validateRequest(request);
        User loginUser = getLoginUser();

        LocalDate targetDate = validateDate(request.getTargetDate(), "목표 마감일");
        LocalDate startDate = request.getStartDate() == null
                ? targetDate
                : validateDate(request.getStartDate(), "목표 시작일");
        validateSchedule(startDate, targetDate);

        Goal goal = new Goal();
        goal.setUser(loginUser);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(normalizeDescription(request.getDescription()));
        goal.setStartDate(startDate);
        goal.setTargetDate(targetDate);
        goal.setProgress(normalizeProgress(request.getProgress()));
        goal.setStatus(normalizeStatus(request.getStatus(), request.getProgress()));
        normalizeCompletedState(goal);

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(Long id, GoalRequest request) {
        validateId(id);
        validateRequest(request);
        User loginUser = getLoginUser();
        Goal goal = findOwnedGoal(id, loginUser.getId());

        LocalDate targetDate = validateDate(request.getTargetDate(), "목표 마감일");
        LocalDate startDate = request.getStartDate() == null
                ? effectiveStartDate(goal)
                : validateDate(request.getStartDate(), "목표 시작일");
        validateSchedule(startDate, targetDate);

        goal.setTitle(request.getTitle().trim());
        goal.setDescription(normalizeDescription(request.getDescription()));
        goal.setStartDate(startDate);
        goal.setTargetDate(targetDate);
        goal.setProgress(normalizeProgress(request.getProgress()));
        goal.setStatus(normalizeStatus(request.getStatus(), request.getProgress()));
        normalizeCompletedState(goal);

        return toResponse(goal);
    }

    /** Calendar move/resize update. Does not touch title, description or progress. */
    @Transactional
    public GoalResponse updateSchedule(Long id, GoalScheduleRequest request) {
        validateId(id);
        if (request == null || request.getStartDate() == null || request.getTargetDate() == null) {
            throw new IllegalArgumentException("목표 시작일과 마감일을 모두 선택해 주세요.");
        }

        LocalDate startDate = validateDate(request.getStartDate(), "목표 시작일");
        LocalDate targetDate = validateDate(request.getTargetDate(), "목표 마감일");
        validateSchedule(startDate, targetDate);

        User loginUser = getLoginUser();
        Goal goal = findOwnedGoal(id, loginUser.getId());
        goal.setStartDate(startDate);
        goal.setTargetDate(targetDate);
        return toResponse(goal);
    }

    /** Slider update from the goal list. */
    @Transactional
    public GoalResponse updateProgress(Long id, GoalProgressRequest request) {
        validateId(id);
        if (request == null || request.getProgress() == null) {
            throw new IllegalArgumentException("진행률을 입력해 주세요.");
        }

        int progress = normalizeProgress(request.getProgress());
        User loginUser = getLoginUser();
        Goal goal = findOwnedGoal(id, loginUser.getId());

        goal.setProgress(progress);
        if (progress >= 100) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else if (progress <= 0) {
            goal.setStatus(GoalStatus.PLANNED);
        } else {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        return toResponse(goal);
    }

    @Transactional
    public void delete(Long id) {
        validateId(id);
        User loginUser = getLoginUser();
        goalRepository.delete(findOwnedGoal(id, loginUser.getId()));
    }

    private Goal findOwnedGoal(Long id, Long userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "목표를 찾을 수 없거나 접근 권한이 없습니다."
                ));
    }

    private void validateRequest(GoalRequest request) {
        if (request == null) throw new IllegalArgumentException("요청 데이터가 없습니다.");
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("목표 제목을 입력해 주세요.");
        }
        if (request.getTitle().trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("목표 제목은 120자 이하로 입력해 주세요.");
        }
        if (request.getDescription() != null && request.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("목표 설명은 3000자 이하로 입력해 주세요.");
        }
        if (request.getTargetDate() == null) {
            throw new IllegalArgumentException("목표 마감일을 선택해 주세요.");
        }
        Integer progress = request.getProgress();
        if (progress != null && (progress < 0 || progress > 100)) {
            throw new IllegalArgumentException("진행률은 0~100 사이여야 합니다.");
        }
        if (request.getStartDate() != null) {
            validateDate(request.getStartDate(), "목표 시작일");
        }
        validateDate(request.getTargetDate(), "목표 마감일");
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("올바른 목표 ID가 필요합니다.");
        }
    }

    private LocalDate validateDate(LocalDate date, String label) {
        if (date == null) throw new IllegalArgumentException(label + "을 선택해 주세요.");
        if (date.getYear() < MIN_YEAR || date.getYear() > MAX_YEAR) {
            throw new IllegalArgumentException(label + "은 2000~2100 사이여야 합니다.");
        }
        return date;
    }

    private void validateSchedule(LocalDate startDate, LocalDate targetDate) {
        if (startDate.isAfter(targetDate)) {
            throw new IllegalArgumentException("목표 시작일은 마감일보다 늦을 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(startDate, targetDate) > MAX_PLAN_DAYS) {
            throw new IllegalArgumentException("목표 기간은 10년 이하로 설정해 주세요.");
        }
    }

    private int normalizeProgress(Integer progress) {
        if (progress == null) return 0;
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("진행률은 0~100 사이여야 합니다.");
        }
        return progress;
    }

    private GoalStatus normalizeStatus(GoalStatus status, Integer progress) {
        int normalizedProgress = normalizeProgress(progress);
        if (normalizedProgress >= 100) return GoalStatus.COMPLETED;
        if (status != null) return status;
        return normalizedProgress > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.PLANNED;
    }

    private void normalizeCompletedState(Goal goal) {
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            goal.setProgress(100);
        } else if (goal.getProgress() >= 100) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private LocalDate effectiveStartDate(Goal goal) {
        return goal.getStartDate() == null ? goal.getTargetDate() : goal.getStartDate();
    }

    private GoalResponse toResponse(Goal goal) {
        LocalDate startDate = effectiveStartDate(goal);
        boolean overdue = goal.getTargetDate() != null
                && goal.getTargetDate().isBefore(LocalDate.now())
                && goal.getStatus() != GoalStatus.COMPLETED;

        return new GoalResponse(
                goal.getId(), goal.getTitle(), goal.getDescription(), startDate, goal.getTargetDate(),
                goal.getStatus(), goal.getProgress(), overdue, goal.getCreatedAt(), goal.getUpdatedAt()
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
