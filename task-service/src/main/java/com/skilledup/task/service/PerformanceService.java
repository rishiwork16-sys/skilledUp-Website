package com.skilledup.task.service;

import com.skilledup.task.model.Submission;
import com.skilledup.task.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PerformanceService {

    private final SubmissionRepository submissionRepository;

    public Double calculateStudentPerformance(Long studentId) {
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);

        if (submissions.isEmpty()) {
            return 0.0;
        }

        long gradedCount = submissions.stream()
                .filter(s -> s.getScore() != null)
                .count();

        if (gradedCount == 0) {
            return 0.0;
        }

        double totalScore = submissions.stream()
                .map(Submission::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return totalScore / gradedCount;
    }

    public boolean isEligibleForRecommendation(Long studentId) {
        Double performance = calculateStudentPerformance(studentId);
        log.info("Performance for student {}: {}%", studentId, performance);
        return performance >= 95.0;
    }
}
