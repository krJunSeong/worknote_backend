package com.example.demo.goal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.goal.entity.Goal;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByTargetDateAscCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    List<Goal> findByUserIdAndTargetDateBetweenOrderByTargetDateAscCreatedAtAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
