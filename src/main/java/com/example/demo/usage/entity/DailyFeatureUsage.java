package com.example.demo.usage.entity;

import java.time.LocalDate;

import com.example.demo.usage.model.UsageFeature;
import com.example.demo.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "daily_feature_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_feature_usage_user_date_feature",
                columnNames = {"user_id", "usage_date", "feature"}
        )
)
public class DailyFeatureUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UsageFeature feature;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    protected DailyFeatureUsage() {
    }

    public DailyFeatureUsage(
            User user,
            LocalDate usageDate,
            UsageFeature feature,
            int usageCount
    ) {
        this.user = user;
        this.usageDate = usageDate;
        this.feature = feature;
        this.usageCount = usageCount;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public UsageFeature getFeature() {
        return feature;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
}
