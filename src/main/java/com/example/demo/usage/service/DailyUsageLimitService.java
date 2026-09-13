package com.example.demo.usage.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.exception.DailyUsageLimitExceededException;
import com.example.demo.usage.entity.DailyFeatureUsage;
import com.example.demo.usage.model.UsageFeature;
import com.example.demo.usage.repository.DailyFeatureUsageRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;

@Service
public class DailyUsageLimitService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Tokyo");

    private final DailyFeatureUsageRepository usageRepository;
    private final UserRepository userRepository;

    public DailyUsageLimitService(
            DailyFeatureUsageRepository usageRepository,
            UserRepository userRepository
    ) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    @Value("${app.usage-limits.ai-per-user-per-day:20}")
    private int aiDailyLimit;

    @Value("${app.usage-limits.azure-ocr-per-user-per-day:5}")
    private int azureOcrDailyLimit;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void consumeAi() {
        validateLimit(aiDailyLimit);

        User user = getLoginUser();
        LocalDate today = LocalDate.now(APP_ZONE);
        DailyFeatureUsage aiUsage = getOrCreateUsage(
                user,
                today,
                UsageFeature.AI
        );

        ensureBelowLimit(aiUsage, aiDailyLimit);
        aiUsage.setUsageCount(aiUsage.getUsageCount() + 1);
        usageRepository.save(aiUsage);
    }

    /**
     * 이미지 초안 생성은 Azure OCR과 AI를 모두 사용하므로 한 요청에서
     * 두 사용량을 같은 트랜잭션으로 확인하고 차감한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void consumeImageDraft() {
        validateLimit(aiDailyLimit);
        validateLimit(azureOcrDailyLimit);

        User user = getLoginUser();
        LocalDate today = LocalDate.now(APP_ZONE);

        DailyFeatureUsage aiUsage = getOrCreateUsage(
                user,
                today,
                UsageFeature.AI
        );
        DailyFeatureUsage ocrUsage = getOrCreateUsage(
                user,
                today,
                UsageFeature.AZURE_OCR
        );

        ensureBelowLimit(aiUsage, aiDailyLimit);
        ensureBelowLimit(ocrUsage, azureOcrDailyLimit);

        aiUsage.setUsageCount(aiUsage.getUsageCount() + 1);
        ocrUsage.setUsageCount(ocrUsage.getUsageCount() + 1);

        usageRepository.saveAll(List.of(aiUsage, ocrUsage));
    }

    private DailyFeatureUsage getOrCreateUsage(
            User user,
            LocalDate date,
            UsageFeature feature
    ) {
        return usageRepository
                .findByUserIdAndUsageDateAndFeature(
                        user.getId(),
                        date,
                        feature
                )
                .orElseGet(() -> new DailyFeatureUsage(
                        user,
                        date,
                        feature,
                        0
                ));
    }

    private void ensureBelowLimit(
            DailyFeatureUsage usage,
            int limit
    ) {
        if (usage.getUsageCount() >= limit) {
            throw new DailyUsageLimitExceededException();
        }
    }

    private User getLoginUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        return userRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "로그인 사용자를 찾을 수 없습니다."
                ));
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalStateException(
                    "일일 AI 사용량 제한 설정은 1 이상이어야 합니다."
            );
        }
    }
}
