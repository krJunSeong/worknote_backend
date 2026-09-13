package com.example.demo.work.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.dto.AiAnalysisResponse;
import com.example.demo.ai.service.OllamaService;
import com.example.demo.usage.service.DailyUsageLimitService;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.work.dto.WorkLogRequest;
import com.example.demo.work.entity.WorkLog;
import com.example.demo.work.repository.WorkLogRepository;
import com.example.demo.work.response.WorkLogResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkLogService {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 20_000;

    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;
    private final DailyUsageLimitService dailyUsageLimitService;

    @Transactional
    public void save(WorkLogRequest request) {

        validateRequest(request);

        User loginUser = getLoginUser();

        validateRequestedUser(
                loginUser,
                request.getUserId()
        );

        dailyUsageLimitService.consumeAi();

        AiAnalysisResponse ai =
                analyzeWorkLog(request);

        WorkLog workLog = WorkLog.builder()
                .user(loginUser)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .aiSummary(
                        getSafeText(ai.getSummary())
                )
                .techTags(
                        joinList(
                                ai.getTechTags(),
                                ", "
                        )
                )
                .interviewQuestions(
                        joinList(
                                ai.getQuestions(),
                                "\n"
                        )
                )
                .difficulty(
                        getSafeDifficulty(
                                ai.getDifficulty()
                        )
                )
                .build();

        workLogRepository.save(workLog);
    }

    public List<WorkLogResponse> findAll(
            Long userId
    ) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "올바른 사용자 ID가 필요합니다."
            );
        }

        User loginUser = getLoginUser();

        validateRequestedUser(
                loginUser,
                userId
        );

        return workLogRepository
                .findByUserId(loginUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void update(
            Long id,
            WorkLogRequest request
    ) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "올바른 업무일지 ID가 필요합니다."
            );
        }

        validateRequest(request);

        User loginUser = getLoginUser();

        validateRequestedUser(
                loginUser,
                request.getUserId()
        );

        WorkLog workLog = workLogRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "업무일지가 존재하지 않습니다."
                        )
                );

        validateOwner(
                workLog,
                loginUser
        );

        dailyUsageLimitService.consumeAi();

        AiAnalysisResponse ai;

        try {
            ai = analyzeWorkLog(request);
        } catch (Exception e) {
            e.printStackTrace();

            ai = new AiAnalysisResponse();
            ai.setSummary("");
            ai.setTechTags(List.of());
            ai.setQuestions(List.of());
            ai.setDifficulty("Nothing");
        }


        workLog.setTitle(
                request.getTitle().trim()
        );

        workLog.setContent(
                request.getContent().trim()
        );

        workLog.setAiSummary(
                getSafeText(ai.getSummary())
        );

        workLog.setTechTags(
                joinList(
                        ai.getTechTags(),
                        ", "
                )
        );

        workLog.setInterviewQuestions(
                joinList(
                        ai.getQuestions(),
                        "\n"
                )
        );

        workLog.setDifficulty(
                getSafeDifficulty(
                        ai.getDifficulty()
                )
        );
    }

    @Transactional
    public void delete(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "올바른 업무일지 ID가 필요합니다."
            );
        }

        User loginUser = getLoginUser();

        WorkLog workLog = workLogRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "업무일지가 존재하지 않습니다."
                        )
                );

        validateOwner(
                workLog,
                loginUser
        );

        workLogRepository.delete(workLog);
    }

    private User getLoginUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new RuntimeException(
                    "로그인이 필요합니다."
            );
        }

        return userRepository
                .findByLoginId(
                        authentication.getName()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "로그인 사용자를 찾을 수 없습니다."
                        )
                );
    }

    private void validateRequestedUser(
            User loginUser,
            Long requestedUserId
    ) {

        if (requestedUserId == null
                || !loginUser.getId()
                        .equals(requestedUserId)) {

            throw new AccessDeniedException(
                    "접근 권한이 없습니다."
            );
        }
    }

    private void validateOwner(
            WorkLog workLog,
            User loginUser
    ) {

        if (workLog.getUser() == null
                || workLog.getUser().getId() == null
                || !workLog.getUser()
                        .getId()
                        .equals(loginUser.getId())) {

            throw new AccessDeniedException(
                    "업무일지에 접근할 권한이 없습니다."
            );
        }
    }

    private AiAnalysisResponse analyzeWorkLog(
            WorkLogRequest request
    ) {

        String fullText = """
                제목:
                %s

                업무 내용:
                %s
                """.formatted(
                request.getTitle(),
                request.getContent()
        );

        return ollamaService.analyze(
                fullText,
                normalizeLanguage(
                        request.getLanguage()
                )
        );
    }

    private void validateRequest(
            WorkLogRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "요청 데이터가 없습니다."
            );
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException(
                    "사용자 ID가 필요합니다."
            );
        }

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "제목을 입력해 주세요."
            );
        }

        if (request.getContent() == null
                || request.getContent().isBlank()) {

            throw new IllegalArgumentException(
                    "업무 내용을 입력해 주세요."
            );
        }

        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException(
                    "올바른 사용자 ID가 필요합니다."
            );
        }

        String title = request.getTitle().trim();
        String content = request.getContent().trim();

        if (title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "제목은 200자 이하로 입력해 주세요."
            );
        }

        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "업무 내용은 20000자 이하로 입력해 주세요."
            );
        }
    }

    private String normalizeLanguage(
            String language
    ) {

        if ("ja".equalsIgnoreCase(language)) {
            return "ja";
        }

        return "ko";
    }

    private String joinList(
            List<String> values,
            String delimiter
    ) {

        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .filter(value -> value != null)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce(
                        (first, second) ->
                                first + delimiter + second
                )
                .orElse("");
    }

    private String getSafeText(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String getSafeDifficulty(
            String difficulty
    ) {

        if (difficulty == null
                || difficulty.isBlank()) {

            return "미분류";
        }

        return difficulty.trim();
    }

    private WorkLogResponse toResponse(
            WorkLog workLog
    ) {

        return WorkLogResponse.builder()
                .id(workLog.getId())
                .title(workLog.getTitle())
                .content(workLog.getContent())
                .aiSummary(workLog.getAiSummary())
                .techTags(workLog.getTechTags())
                .interviewQuestions(
                        workLog.getInterviewQuestions()
                )
                .difficulty(workLog.getDifficulty())
                .createdAt(workLog.getCreatedAt())
                .build();
    }
}
