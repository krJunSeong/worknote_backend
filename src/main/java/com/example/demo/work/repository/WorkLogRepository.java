package com.example.demo.work.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.work.entity.WorkLog;

@Repository
public interface WorkLogRepository
        extends JpaRepository<WorkLog, Long> {

    List<WorkLog> findByUserId(Long userId);

    List<WorkLog> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<WorkLog> findByUserIdOrderByCreatedAtAsc(
            Long userId
    );
    List<WorkLog> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

}