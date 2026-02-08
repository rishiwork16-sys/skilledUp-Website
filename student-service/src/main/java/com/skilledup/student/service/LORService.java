package com.skilledup.student.service;

import com.skilledup.student.client.CertificateClient;
import com.skilledup.student.client.NotificationClient;
import com.skilledup.student.client.TaskClient;
import com.skilledup.student.dto.EmailRequest;
import com.skilledup.student.dto.LORGenerationRequest;
import com.skilledup.student.dto.LORRequestDTO;
import com.skilledup.student.dto.TaskCompletionStatsDTO;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.LORRequest;
import com.skilledup.student.model.Student;
import com.skilledup.student.repository.InternshipEnrollmentRepository;
import com.skilledup.student.repository.LORRequestRepository;
import com.skilledup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LORService {

    private final LORRequestRepository lorRequestRepository;
    private final InternshipEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final TaskClient taskClient;
    private final NotificationClient notificationClient;
    private final CertificateClient certificateClient;

    private static final int MINIMUM_DURATION_WEEKS = 24;
    private static final int MINIMUM_COMPLETION_PERCENT = 95;

    /**
     * Check if student is eligible for LOR
     */
    public EligibilityResult checkEligibility(Long studentId, Long enrollmentId) {
        log.info("Checking LOR eligibility for student {} and enrollment {}", studentId, enrollmentId);

        // 1. Get enrollment
        InternshipEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Enrollment does not belong to this student");
        }

        EligibilityResult result = new EligibilityResult();
        result.setEligible(true);

        // 2. Check duration >= 24 weeks
        if (enrollment.getDuration() < MINIMUM_DURATION_WEEKS) {
            result.setEligible(false);
            result.setReason("Internship duration must be at least " + MINIMUM_DURATION_WEEKS + " weeks. Current: "
                    + enrollment.getDuration() + " weeks");
            return result;
        }

        // 3. Check if internship has ended (Optional/Strict check)
        // LocalDate today = LocalDate.now();
        // if (enrollment.getEndDate() != null &&
        // enrollment.getEndDate().isAfter(today)) {
        // result.setEligible(false);
        // result.setReason("Internship must be completed before LOR request.");
        // // returning result here strictness depends on req. User said "jb student
        // sabhi metrics complete karega"
        // // usually implies duration/tasks. Date might be flexible or strict. keeping
        // strict.
        // // return result;
        // }
        // Uncommenting date check if required. For now trusting duration/tasks.

        // 4. Check task completion >= 95%
        try {
            // Use title as domain identifier
            TaskCompletionStatsDTO stats = taskClient.getTaskCompletionStats(
                    studentId,
                    enrollment.getInternshipCategory().getTitle());
            result.setTaskStats(stats);

            if (stats.getCompletionPercent() < MINIMUM_COMPLETION_PERCENT) {
                result.setEligible(false);
                result.setReason("Task completion must be at least " + MINIMUM_COMPLETION_PERCENT + "%. Current: "
                        + stats.getCompletionPercent() + "%");
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to fetch task completion stats", e);
            result.setEligible(false);
            result.setReason("Unable to verify task completion. Please try again later.");
            return result;
        }

        result.setReason("Eligible for LOR");
        return result;
    }

    /**
     * Student requests LOR - Auto Approved & Generated if Eligible
     */
    @Transactional
    public LORRequest requestLOR(Long studentId, LORRequestDTO requestDTO) {
        log.info("Processing LOR request for student {} and enrollment {}", studentId, requestDTO.getEnrollmentId());

        // Check if already requested
        if (lorRequestRepository.existsByStudentIdAndEnrollmentId(studentId, requestDTO.getEnrollmentId())) {
            throw new RuntimeException("LOR already requested for this internship");
        }

        // Check eligibility
        EligibilityResult eligibility = checkEligibility(studentId, requestDTO.getEnrollmentId());
        if (!eligibility.isEligible()) {
            throw new RuntimeException("Not eligible for LOR: " + eligibility.getReason());
        }

        // Auto-Generate LOR ID
        String uniqueLorId = generateUniqueLorId(studentId);

        // Prepare Generation Request
        InternshipEnrollment enrollment = enrollmentRepository.findById(requestDTO.getEnrollmentId()).orElseThrow();
        Student student = studentRepository.findById(studentId).orElseThrow();

        // Determine End Date (Last Submitted Task Date)
        LocalDate endDate = enrollment.getEndDate();
        try {
            List<Object> submissions = taskClient.getMySubmissions(studentId);
            if (submissions != null && !submissions.isEmpty()) {
                // Find latest submission date
                // Submissions are Objects (LinkedHashMap), we need to parse 'submittedAt'
                java.time.Instant latestSubmission = null;

                for (Object subObj : submissions) {
                    if (subObj instanceof java.util.Map) {
                        java.util.Map<?, ?> subMap = (java.util.Map<?, ?>) subObj;
                        Object submittedAtObj = subMap.get("submittedAt");
                        if (submittedAtObj != null) {
                            java.time.Instant currentInstant = null;
                            if (submittedAtObj instanceof String) {
                                currentInstant = java.time.Instant.parse((String) submittedAtObj);
                            } else if (submittedAtObj instanceof Number) {
                                currentInstant = java.time.Instant.ofEpochSecond(((Number) submittedAtObj).longValue());
                            }

                            if (currentInstant != null) {
                                if (latestSubmission == null || currentInstant.isAfter(latestSubmission)) {
                                    latestSubmission = currentInstant;
                                }
                            }
                        }
                    }
                }

                if (latestSubmission != null) {
                    endDate = latestSubmission.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    log.info("Found last submission date: {}", endDate);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch submissions for End Date calculation, using enrollment end date", e);
        }

        LORGenerationRequest genRequest = LORGenerationRequest.builder()
                .studentName(student.getName())
                .email(student.getEmail())
                .domain(enrollment.getInternshipCategory().getTitle())
                .startDate(enrollment.getStartDate())
                .endDate(endDate) // Dynamic End Date
                .duration(enrollment.getDuration())
                .completionPercent(eligibility.getTaskStats().getCompletionPercent().doubleValue())
                .lorId(uniqueLorId)
                .build();

        // Call Certificate Service to Generate
        String lorUrl;
        try {
            lorUrl = certificateClient.generateLOR(genRequest);
            log.info("LOR Generated URL: {}", lorUrl);
        } catch (Exception e) {
            log.error("Failed to generate LOR via Certificate Service", e);
            throw new RuntimeException(
                    "Failed to generate LOR document. Verification successful but generation failed.");
        }

        // Create Request Record as GENERATED
        LORRequest lorRequest = LORRequest.builder()
                .studentId(studentId)
                .enrollmentId(requestDTO.getEnrollmentId())
                .requestStatus(LORRequest.RequestStatus.GENERATED)
                .eligibilityChecked(true)
                .taskCompletionPercent(eligibility.getTaskStats().getCompletionPercent())
                .totalTasks(eligibility.getTaskStats().getTotalTasks())
                .completedTasks(eligibility.getTaskStats().getCompletedTasks())
                .requestedAt(Instant.now())
                .approvedAt(Instant.now()) // Auto-approved
                .generatedAt(Instant.now())
                .uniqueLorId(uniqueLorId)
                .lorUrl(lorUrl)
                .adminRemarks("Auto-approved & Generated: Strategy v2")
                .build();

        LORRequest saved = lorRequestRepository.save(lorRequest);
        log.info("LOR request saved with ID: {}", saved.getId());

        // Send Email
        sendLORReadyEmail(saved);

        return saved;
    }

    /**
     * Get all LOR requests for a student
     */
    public List<LORRequest> getMyRequests(Long studentId) {
        return lorRequestRepository.findByStudentId(studentId);
    }

    /**
     * Get LOR request by ID
     */
    public LORRequest getRequestById(Long requestId) {
        return lorRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("LOR request not found"));
    }

    // Legacy Admin methods removed as per requirement for Auto-Approval logic.

    private String generateUniqueLorId(Long studentId) {
        // Format: INTYYMM00001 (INT + Year + Month + 5-digit Sequence/StudentId)
        // Note: Using studentId directly as sequence ensuring uniqueness per user.
        // For strict 00001 sequence per month, we would need a DB counter.
        // Assuming studentId is sufficient for uniqueness.
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear()).substring(2); // Last 2 digits (e.g., 25)
        String month = String.format("%02d", now.getMonthValue()); // 2 digits (e.g., 10)
        return String.format("INT%s%s%05d", year, month, studentId);
    }

    // Email notification methods
    private void sendLORReadyEmail(LORRequest request) {
        try {
            Student student = studentRepository.findById(request.getStudentId()).orElse(null);
            if (student == null || student.getEmail() == null) {
                return;
            }

            String subject = "Your Letter of Recommendation is Ready!";
            String body = String.format(
                    "Dear %s,\n\n" +
                            "Congratulations! Your Letter of Recommendation has been generated automatically upon completing your internship criteria.\n\n"
                            +
                            "LOR ID: %s\n" +
                            "Download Link: %s\n\n" +
                            "This letter is valid for all professional purposes.\n\n" +
                            "Regards,\nSkilledUp Team",
                    student.getName(),
                    request.getUniqueLorId(),
                    request.getLorUrl());

            EmailRequest emailRequest = EmailRequest.builder()
                    .recipient(student.getEmail())
                    .subject(subject)
                    .body(body)
                    .build();

            notificationClient.sendEmail(emailRequest);
            log.info("Sent LOR ready email to {}", student.getEmail());

        } catch (Exception e) {
            log.error("Failed to send LOR ready email", e);
        }
    }

    // TESTING ONLY: Reset LOR Request
    @Transactional
    public void resetLOR(Long studentId, Long enrollmentId) {
        LORRequest request = lorRequestRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        lorRequestRepository.delete(request);
        log.info("Reset LOR request for student {} enrollment {}", studentId, enrollmentId);
    }

    // Inner class for eligibility result
    @lombok.Data
    public static class EligibilityResult {
        private boolean eligible;
        private String reason;
        private TaskCompletionStatsDTO taskStats;
    }
}
