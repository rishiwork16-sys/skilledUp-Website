package com.skilledup.task.scheduler;

import com.skilledup.task.client.NotificationClient;
import com.skilledup.task.client.StudentClient;
import com.skilledup.task.dto.EmailRequest;
import com.skilledup.task.dto.StudentResponse;
import com.skilledup.task.model.TaskSchedule;
import com.skilledup.task.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final TaskScheduleRepository taskScheduleRepository;
    private final StudentClient studentClient;
    private final NotificationClient notificationClient;

    // Run every minute for testing (SHOULD BE HOURLY IN PROD)
    @Scheduled(cron = "0 * * * * *")
    // @SchedulerLock(name = "TaskReminderScheduler_sendTaskOverdueReminders",
    // lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    public void sendTaskOverdueReminders() {
        log.info("Running Task Overdue Reminder Scheduler with Locking & Batching...");
        LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);
        log.info("Current Scheduler Time (now): {}", now);

        int count = taskScheduleRepository.findAll().size();
        log.info("Total TaskSchedule records in DB: {}", count);
        int pageNumber = 0;
        int pageSize = 100;
        Page<TaskSchedule> page;

        do {
            page = taskScheduleRepository.findByIsSubmittedFalseAndIsUnlockedTrueAndDeadlineBefore(
                    now,
                    PageRequest.of(pageNumber, pageSize, Sort.by("id").ascending()) // Predictable order for batching
            );

            log.info("Processing batch {}/{} ({} tasks)", pageNumber + 1, page.getTotalPages(),
                    page.getNumberOfElements());

            for (TaskSchedule schedule : page.getContent()) {
                try {
                    processReminder(schedule, now);
                } catch (Exception e) {
                    log.error("Failed to process reminder for schedule ID {}", schedule.getId(), e);
                }
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Completed Overdue Reminder Scheduler run.");
    }

    private void processReminder(TaskSchedule schedule, LocalDateTime now) {
        boolean shouldSendEmail = false;

        if (schedule.getLastReminderSentAt() == null) {
            // Case 1: First reminder immediately after deadline
            shouldSendEmail = true;
        } else {
            // Case 2: Recurring reminder every 3 days
            long daysSinceLastReminder = ChronoUnit.DAYS.between(schedule.getLastReminderSentAt(), now);
            if (daysSinceLastReminder >= 3) {
                shouldSendEmail = true;
            }
        }

        if (shouldSendEmail) {
            log.info("Sending email for scheduleId: {}", schedule.getId());
            boolean emailSent = sendEmail(schedule);
            if (emailSent) {
                schedule.setLastReminderSentAt(now);
                taskScheduleRepository.save(schedule);
            } else {
                log.warn("Email sending failed for scheduleId: {}. Will retry next cycle.", schedule.getId());
            }
        } else {
            log.info("Skipping email for scheduleId: {}. Last sent: {}", schedule.getId(),
                    schedule.getLastReminderSentAt());
        }
    }

    private boolean sendEmail(TaskSchedule schedule) {
        try {
            Long studentId = schedule.getStudentId();
            StudentResponse student = studentClient.getStudentById(studentId);

            if (student == null || student.getEmail() == null) {
                log.error("Student or email not found for ID {}", studentId);
                return false;
            }

            String subject = "Task Overdue – Please Submit Immediately";
            String body = String.format(
                    "Dear %s,\n\n" +
                            "Your task '%s' was due on %s. You have not submitted it yet.\n\n" +
                            "Please submit the task immediately to avoid further delays. " +
                            "You will receive reminders every 3 days until you submit.\n\n" +
                            "Regards,\nSkilledUp Team",
                    student.getName(),
                    schedule.getTask().getTitle(),
                    schedule.getDeadline().toString());

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setRecipient(student.getEmail());
            emailRequest.setSubject(subject);
            emailRequest.setBody(body);

            notificationClient.sendEmail(emailRequest);
            log.info("Sent overdue reminder to {} for task {}", student.getEmail(), schedule.getTask().getTitle());
            return true;

        } catch (Exception e) {
            log.error("Error sending email for schedule {}", schedule.getId(), e);
            return false;
        }
    }
}
