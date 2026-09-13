package com.example.demo.usage.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.usage.entity.DailyFeatureUsage;
import com.example.demo.usage.model.UsageFeature;


@Repository
public interface DailyFeatureUsageRepository
        extends JpaRepository<DailyFeatureUsage, Long> {

    Optional<DailyFeatureUsage> findByUserIdAndUsageDateAndFeature(
            Long userId,
            LocalDate usageDate,
            UsageFeature feature
    );
}
