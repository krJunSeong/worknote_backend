package com.example.demo.goal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.goal.entity.Goal;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByTargetDateAscCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select g
            from Goal g
            where g.user.id = :userId
              and coalesce(g.startDate, g.targetDate) <= :endDate
              and g.targetDate >= :startDate
            order by coalesce(g.startDate, g.targetDate) asc, g.createdAt asc
            """)
    List<Goal> findOverlappingCalendarRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
