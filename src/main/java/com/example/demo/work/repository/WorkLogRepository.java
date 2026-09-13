package com.example.demo.work.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.work.entity.WorkLog;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    List<WorkLog> findByUserId(Long userId);

    List<WorkLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WorkLog> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<WorkLog> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select w
            from WorkLog w
            where w.user.id = :userId
              and (
                    (w.workDate is not null and w.workDate between :startDate and :endDate)
                    or
                    (w.workDate is null and w.createdAt >= :startDateTime and w.createdAt < :endDateTime)
                  )
            order by w.createdAt asc
            """)
    List<WorkLog> findCalendarWorkLogs(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
