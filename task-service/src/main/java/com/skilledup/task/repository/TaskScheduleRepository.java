package com.skilledup.task.repository;

import com.skilledup.task.model.TaskSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskScheduleRepository extends JpaRepository<TaskSchedule, Long> {

        List<TaskSchedule> findByStudentId(Long studentId);

        Optional<TaskSchedule> findByStudentIdAndTaskId(Long studentId, Long taskId);

        void deleteByTaskId(Long taskId);

        List<TaskSchedule> findByUnlockDateAndIsUnlockedFalse(LocalDate unlockDate);

        List<TaskSchedule> findByDeadlineBeforeAndIsSubmittedFalseAndIsDelayedFalseAndIsUnlockedTrue(
                        LocalDateTime deadline);

        // Find overdue tasks (submitted=false, unlocked=true, deadline < now) - with
        // pagination
        org.springframework.data.domain.Page<TaskSchedule> findByIsSubmittedFalseAndIsUnlockedTrueAndDeadlineBefore(
                        LocalDateTime now, org.springframework.data.domain.Pageable pageable);

        @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END FROM TaskSchedule ts WHERE ts.studentId = :studentId AND ts.task.domain = :domain AND ts.task.weekNo = :weekNo AND ts.isSubmitted = true")
        boolean isPreviousTaskCompleted(@Param("studentId") Long studentId,
                        @Param("domain") String domain,
                        @Param("weekNo") Integer weekNo);
}
