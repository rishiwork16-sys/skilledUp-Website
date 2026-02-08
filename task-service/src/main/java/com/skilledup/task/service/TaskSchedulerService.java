package com.skilledup.task.service;

import com.skilledup.task.client.NotificationClient;
import com.skilledup.task.dto.EmailRequest;
import com.skilledup.task.model.TaskSchedule;
import com.skilledup.task.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSchedulerService {

    private final TaskScheduleRepository taskScheduleRepository;
    private final NotificationClient notificationClient;
    private final com.skilledup.task.client.StudentClient studentClient;
    // We now have StudentClient to lookup email.

    /**
     * Runs daily at Midnight to unlock tasks scheduled for today (Mondays).
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void unlockScheduledTasks() {
        LocalDate today = LocalDate.now();
        log.info("Running scheduled unlock for date: {}", today);

        List<TaskSchedule> schedulesToUnlock = taskScheduleRepository.findByUnlockDateAndIsUnlockedFalse(today);

        for (TaskSchedule schedule : schedulesToUnlock) {
            boolean canUnlock = checkPrerequisites(schedule);

            if (canUnlock) {
                schedule.setUnlocked(true);
                taskScheduleRepository.save(schedule);
                log.info("Unlocked task {} for student {}", schedule.getTask().getTitle(), schedule.getStudentId());

                // Send Email Alert
                sendTaskUnlockedEmail(schedule);
            }
        }
    }

    /**
     * Runs every hour to check for missed deadlines.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkDeadlines() {
        LocalDateTime now = LocalDateTime.now();

        List<TaskSchedule> lateTasks = taskScheduleRepository
                .findByDeadlineBeforeAndIsSubmittedFalseAndIsDelayedFalseAndIsUnlockedTrue(now);

        for (TaskSchedule schedule : lateTasks) {
            schedule.setDelayed(true);
            taskScheduleRepository.save(schedule);
            log.info("Marked task {} as DELAYED for student {}", schedule.getTask().getTitle(),
                    schedule.getStudentId());

            // Send Email Alert
            sendTaskDelayedEmail(schedule);
        }
    }

    private boolean checkPrerequisites(TaskSchedule schedule) {
        if (schedule.getTask().getWeekNo() == 1)
            return true; // Week 1 has no prerequisite

        Integer prevWeek = schedule.getTask().getWeekNo() - 1;
        String domain = schedule.getTask().getDomain();
        Long studentId = schedule.getStudentId();

        return taskScheduleRepository.isPreviousTaskCompleted(studentId, domain, prevWeek);
    }

    private void sendTaskUnlockedEmail(TaskSchedule schedule) {
        try {
            com.skilledup.task.dto.StudentResponse student = studentClient.getStudentById(schedule.getStudentId());
            if (student != null && student.getEmail() != null) {
                notificationClient.sendEmail(new EmailRequest(student.getEmail(), "New Task Unlocked",
                        "Task " + schedule.getTask().getTitle() + " is now available."));
                log.info("Sent Task Unlocked Email to {}", student.getEmail());
            } else {
                log.warn("Could not find student email for ID {}", schedule.getStudentId());
            }
        } catch (Exception e) {
            log.error("Failed to send unlocked email", e);
        }
    }

    private void sendTaskDelayedEmail(TaskSchedule schedule) {
        try {
            com.skilledup.task.dto.StudentResponse student = studentClient.getStudentById(schedule.getStudentId());
            if (student != null && student.getEmail() != null) {
                notificationClient.sendEmail(new EmailRequest(student.getEmail(), "Task Deadline Missed",
                        "Your task " + schedule.getTask().getTitle() + " is now marked as Delayed."));
                log.info("Sent Task Delayed Email to {}", student.getEmail());
            } else {
                log.warn("Could not find student email for ID {}", schedule.getStudentId());
            }
        } catch (Exception e) {
            log.error("Failed to send delayed email", e);
        }
    }
}
