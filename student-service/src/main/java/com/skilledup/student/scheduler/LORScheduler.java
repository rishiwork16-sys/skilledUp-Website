package com.skilledup.student.scheduler;

import com.skilledup.student.client.NotificationClient;
import com.skilledup.student.dto.EmailRequest;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.LORRequest;
import com.skilledup.student.model.Student;
import com.skilledup.student.repository.InternshipEnrollmentRepository;
import com.skilledup.student.repository.LORRequestRepository;
import com.skilledup.student.repository.StudentRepository;
import com.skilledup.student.service.LORService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LORScheduler {

    private final LORRequestRepository lorRequestRepository;
    private final InternshipEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final LORService lorService;
    private final NotificationClient notificationClient;

    /**
     * Daily at 2 AM - Auto-generate LOR PDFs for approved requests
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void autoGenerateLORs() {
        log.info("Starting auto-generation of LOR PDFs...");

        LocalDate today = LocalDate.now();
        List<LORRequest> eligibleRequests = lorRequestRepository.findEligibleForAutoGeneration(today);

        log.info("Found {} eligible LOR requests for generation", eligibleRequests.size());

        for (LORRequest request : eligibleRequests) {
            try {
                // Generate unique LOR ID
                String uniqueLorId = generateUniqueLorId(request.getId());

                // TODO: Generate PDF from template
                // String lorUrl = generateAndUploadLOR(request, uniqueLorId);

                // For now, mark as placeholder
                String lorUrl = "https://s3.amazonaws.com/lor-pdfs/" + uniqueLorId + ".pdf";

                // Update request
                request.setLorUrl(lorUrl);
                request.setUniqueLorId(uniqueLorId);
                request.setRequestStatus(LORRequest.RequestStatus.GENERATED);
                request.setGeneratedAt(Instant.now());

                lorRequestRepository.save(request);

                // Send email with download link
                sendLORReadyEmail(request);

                log.info("Generated LOR for request ID: {} with unique ID: {}", request.getId(), uniqueLorId);

            } catch (Exception e) {
                log.error("Failed to generate LOR for request ID: {}", request.getId(), e);
            }
        }

        log.info("Completed auto-generation of LOR PDFs");
    }

    /**
     * Daily at 10 AM - Send eligibility reminders to students
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendEligibilityReminders() {
        log.info("Starting eligibility reminder job...");

        LocalDate sevenDaysLater = LocalDate.now().plusDays(7);
        List<InternshipEnrollment> endingEnrollments = enrollmentRepository.findByEndDate(sevenDaysLater);

        log.info("Found {} enrollments ending in 7 days", endingEnrollments.size());

        for (InternshipEnrollment enrollment : endingEnrollments) {
            try {
                // Check if LOR already requested
                boolean alreadyRequested = lorRequestRepository.existsByStudentIdAndEnrollmentId(
                        enrollment.getStudent().getId(),
                        enrollment.getId());

                if (alreadyRequested) {
                    continue; // Skip if already requested
                }

                // Check eligibility
                LORService.EligibilityResult result = lorService.checkEligibility(
                        enrollment.getStudent().getId(),
                        enrollment.getId());

                if (result.isEligible()) {
                    sendEligibilityReminderEmail(enrollment);
                    log.info("Sent eligibility reminder to student ID: {}", enrollment.getStudent().getId());
                }

            } catch (Exception e) {
                log.error("Failed to send reminder for enrollment ID: {}", enrollment.getId(), e);
            }
        }

        log.info("Completed eligibility reminder job");
    }

    private String generateUniqueLorId(Long requestId) {
        return String.format("LOR-%d-%05d", LocalDate.now().getYear(), requestId);
    }

    private void sendLORReadyEmail(LORRequest request) {
        try {
            Student student = studentRepository.findById(request.getStudentId()).orElse(null);
            if (student == null || student.getEmail() == null) {
                log.warn("Student or email not found for ID {}", request.getStudentId());
                return;
            }

            String subject = "Your Letter of Recommendation is Ready!";
            String body = String.format(
                    "Dear %s,\n\n" +
                            "Great news! Your Letter of Recommendation has been generated and is ready for download.\n\n"
                            +
                            "LOR ID: %s\n" +
                            "Download Link: %s\n\n" +
                            "This letter can be used for job applications, higher studies, or any professional purpose.\n\n"
                            +
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

    private void sendEligibilityReminderEmail(InternshipEnrollment enrollment) {
        try {
            Student student = enrollment.getStudent();
            if (student == null || student.getEmail() == null) {
                log.warn("Student or email not found");
                return;
            }

            String subject = "Complete Your Tasks - LOR Eligibility Reminder";
            String body = String.format(
                    "Dear %s,\n\n" +
                            "Your internship in %s is ending in 7 days (End Date: %s).\n\n" +
                            "You are currently eligible for a Letter of Recommendation! " +
                            "Make sure to complete any remaining tasks to maintain your eligibility.\n\n" +
                            "Eligibility Criteria:\n" +
                            "✅ Duration: %d weeks (minimum 24 weeks required)\n" +
                            "✅ Task Completion: Maintain 95%% or above\n" +
                            "✅ Internship Completion: Complete before end date\n\n" +
                            "You can request your LOR from the student dashboard once your internship is complete.\n\n"
                            +
                            "Regards,\nSkilledUp Team",
                    student.getName(),
                    enrollment.getInternshipCategory().getTitle(),
                    enrollment.getEndDate(),
                    enrollment.getDuration());

            EmailRequest emailRequest = EmailRequest.builder()
                    .recipient(student.getEmail())
                    .subject(subject)
                    .body(body)
                    .build();

            notificationClient.sendEmail(emailRequest);
            log.info("Sent eligibility reminder to {}", student.getEmail());

        } catch (Exception e) {
            log.error("Failed to send eligibility reminder email", e);
        }
    }
}
