package com.skilledup.task.repository;

import com.skilledup.task.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStudentId(Long studentId);

    Optional<Submission> findByStudentIdAndTaskId(Long studentId, Long taskId);

    List<Submission> findByStatus(Submission.SubmissionStatus status);
}
