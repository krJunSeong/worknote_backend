package com.example.demo.goal.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.goal.dto.GoalRequest;
import com.example.demo.goal.dto.GoalResponse;
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

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public List<GoalResponse> findAll() {
        User loginUser = getLoginUser();
        return goalRepository.findByUserIdOrderByTargetDateAscCreatedAtDesc(loginUser.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        validateRequest(request);
        User loginUser = getLoginUser();

        Goal goal = new Goal();
        goal.setUser(loginUser);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(normalizeDescription(request.getDescription()));
        goal.setTargetDate(request.getTargetDate());
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

        goal.setTitle(request.getTitle().trim());
        goal.setDescription(normalizeDescription(request.getDescription()));
        goal.setTargetDate(request.getTargetDate());
        goal.setProgress(normalizeProgress(request.getProgress()));
        goal.setStatus(normalizeStatus(request.getStatus(), request.getProgress()));
        normalizeCompletedState(goal);

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
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("올바른 목표 ID가 필요합니다.");
        }
    }

    private int normalizeProgress(Integer progress) {
        return progress == null ? 0 : progress;
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

    private GoalResponse toResponse(Goal goal) {
        boolean overdue = goal.getTargetDate() != null
                && goal.getTargetDate().isBefore(LocalDate.now())
                && goal.getStatus() != GoalStatus.COMPLETED;

        return new GoalResponse(
                goal.getId(), goal.getTitle(), goal.getDescription(), goal.getTargetDate(),
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
