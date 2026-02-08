package com.skilledup.task.service;

import com.skilledup.task.dto.SubmissionRequest;
import com.skilledup.task.model.Submission;
import com.skilledup.task.model.Task;
import com.skilledup.task.model.TaskSchedule;
import com.skilledup.task.repository.SubmissionRepository;
import com.skilledup.task.repository.TaskRepository;
import com.skilledup.task.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final TaskScheduleRepository taskScheduleRepository;

    private final S3Service s3Service;
    private final com.skilledup.task.client.StudentClient studentClient;

    @org.springframework.beans.factory.annotation.Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public Task createTask(com.skilledup.task.dto.CreateTaskRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .domain(request.getDomain())
                .weekNo(request.getWeekNo())
                .taskFileUrl(request.getTaskFileUrl())
                .videoUrl(request.getVideoUrl())
                .urlFileUrl(request.getUrlFileUrl())
                .deadline(request.getDeadline())
                .startDate(request.getStartDate())
                .isManual(request.getIsManual() != null ? request.getIsManual() : false)
                .autoReview(request.getAutoReview() != null ? request.getAutoReview() : false)
                .active(true)
                .build();
        Task savedTask = taskRepository.save(task);

        // Assign to active students
        assignTaskToActiveStudents(savedTask);

        return savedTask;
    }

    private void assignTaskToActiveStudents(Task task) {
        try {
            List<Long> studentIds = studentClient.getActiveStudentsByDomain(task.getDomain());
            if (studentIds.isEmpty()) {
                log.info("No active students found for domain: {}", task.getDomain());
                return;
            }

            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate week1UnlockDate;
            if (now.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                week1UnlockDate = now;
            } else {
                week1UnlockDate = now.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
            }
            // Logic validation: existing students might have different start dates.
            // But for simplicity/MVP, we'll assume the task week relates to the "Program
            // Week".
            // Ideally we need to know each student's start date to calculate THEIR unlock
            // date.
            // However, the current initializeTaskSchedule logic uses "Next Monday" as Week
            // 1 for EVERYONE engaging.
            // Wait, initializeTaskSchedules calculates unlockDate based on NOW.

            // For existing students, we should probably fetch their enrollment or just use
            // the current logic which assigns it "relative to program start"?
            // Actually `initializeTaskSchedules` sets unlock dates based on Week 1 = Next
            // Monday.

            // If we are adding a NEW task (e.g. Week 1 Task 2), and the student is in Week
            // 3, this task should be unlocked immediately (or have passed).
            // But we don't have the student's start date here.

            // Simplification: We will assign the task with unlock date = task.weekNo
            // relative to "Now" is wrong if they started weeks ago.
            // BUT, `StudentClient` only returns IDs.

            // CRITICAL: We need to know when the student started to calculate valid unlock
            // dates.
            // Or we just set it to "Now" if WeekNo matching?
            // "initializeTaskSchedules" assumes a fresh batch or re-calc.

            // Let's refine the requirement: "Display task to student".
            // If we blindly create a schedule with unlockDate = Now (if week matches) or
            // future.

            // Re-use logic: The `initializeTaskSchedules` logic calculates absolute dates
            // from "Next Monday".
            // This implies a "Batch" system where everyone starts next Monday?
            // If so, then for current students, "Next Monday" might be in the past? No.

            // Let's look at `initializeTaskSchedules` again.
            // `week1UnlockDate = now` if Monday, else `next(Monday)`.
            // So it sets the schedule relative to the CURRENT calendar week.
            // If I am in Week 5 (enrolled 5 weeks ago), and a new Week 1 task is added.
            // If we use the same logic, it will be scheduled for "Next Monday" (Week 1 of
            // 'current time').
            // This means an old student sees a new Week 1 task as "Future".

            // To do this correctly, we'd need the student's Enrolled Date.
            // But we can't get that easily efficiently for all.

            // Workaround: Call `initializeTaskSchedules` for each student found?
            // `initializeTaskSchedules` (in TaskService) fetches ALL tasks for the domain
            // and adds missing schedules.
            // perfect!
            // I just need to iterate IDs and call `initializeTaskSchedules(studentId,
            // task.getDomain())`
            // But wait, `initializeTaskSchedules` re-calculates dates based on "Now".
            // If I call it today for an old student, it will schedule the new task for Next
            // Monday.
            // This is acceptable behavior for "New Content Release".

            studentIds.forEach(studentId -> {
                initializeTaskSchedules(studentId, task.getDomain());
            });

        } catch (Exception e) {
            log.error("Failed to assign new task to students", e);
        }
    }

    public List<TaskSchedule> getMyTasks(Long studentId) {
        List<TaskSchedule> schedules = taskScheduleRepository.findByStudentId(studentId);
        return schedules.stream()
                .peek(schedule -> schedule.setTask(signTaskUrls(schedule.getTask())))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public java.util.Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id).map(this::signTaskUrls);
    }

    private Task signTaskUrls(Task task) {
        if (task == null)
            return null;

        String signedTaskUrl = signIfS3Url(task.getTaskFileUrl());
        String signedVideoUrl = signIfS3Url(task.getVideoUrl());
        String signedUrlFileUrl = signIfS3Url(task.getUrlFileUrl());

        return Task.builder()
                .id(task.getId())
                .domain(task.getDomain())
                .weekNo(task.getWeekNo())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskFileUrl(signedTaskUrl)
                .videoUrl(signedVideoUrl)
                .urlFileUrl(signedUrlFileUrl)
                .startDate(task.getStartDate())
                .deadline(task.getDeadline())
                .isManual(task.isManual())
                .autoReview(task.isAutoReview())
                .active(task.isActive())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private String signIfS3Url(String url) {
        if (url == null || url.isEmpty())
            return url;
        // Only sign if it contains our bucket name, implying it's an S3 object we own
        if (url.contains(bucketName)) {
            try {
                return s3Service.generateSignedUrl(url);
            } catch (Exception e) {
                log.error("Error signing URL: {}", url, e);
                return url; // Fallback to raw URL
            }
        }
        return url; // Return external links as-is
    }

    @Transactional
    public Submission submitTask(SubmissionRequest request) {
        // Find task
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Find task schedule
        TaskSchedule schedule = taskScheduleRepository
                .findByStudentIdAndTaskId(request.getStudentId(), request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task schedule not found"));

        // Check if task is unlocked
        if (!schedule.isUnlocked()) {
            throw new RuntimeException("Task is not unlocked yet");
        }

        // Check if already submitted
        if (schedule.isSubmitted()) {
            throw new RuntimeException("Task already submitted");
        }

        // Determine status based on auto-review setting
        Submission.SubmissionStatus initialStatus = task.isAutoReview()
                ? Submission.SubmissionStatus.APPROVED
                : Submission.SubmissionStatus.PENDING;

        Integer initialScore = task.isAutoReview() ? 100 : null;

        // Create submission
        Submission submission = Submission.builder()
                .studentId(request.getStudentId())
                .task(task)
                .submissionFileUrl(request.getSubmissionFileUrl())
                .status(initialStatus)
                .score(initialScore)
                .submittedAt(java.time.Instant.now())
                .build();

        submission = submissionRepository.save(submission);

        // Update schedule
        schedule.setSubmitted(true);
        taskScheduleRepository.save(schedule);

        // Sequential unlock: Unlock next task
        unlockNextTask(request.getStudentId(), task);

        log.info("Student {} submitted task {}. Auto-Review: {}", request.getStudentId(), task.getTitle(),
                task.isAutoReview());

        return submission;
    }

    private void unlockNextTask(Long studentId, Task currentTask) {
        // Find next week's task in same domain
        Integer nextWeek = currentTask.getWeekNo() + 1;

        taskRepository.findByDomainAndWeekNoAndActiveTrue(currentTask.getDomain(), nextWeek)
                .ifPresent(nextTask -> {
                    taskScheduleRepository.findByStudentIdAndTaskId(studentId, nextTask.getId())
                            .ifPresent(nextSchedule -> {
                                if (!nextSchedule.isUnlocked()) {
                                    nextSchedule.setUnlocked(true);
                                    taskScheduleRepository.save(nextSchedule);
                                    log.info("Auto-unlocked next task {} for student {}",
                                            nextTask.getTitle(), studentId);
                                }
                            });
                });
    }

    public List<Submission> getMySubmissions(Long studentId) {
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        return submissions.stream()
                .map(this::signSubmissionUrl)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Submission> getPendingSubmissions() {
        List<Submission> submissions = submissionRepository.findByStatus(Submission.SubmissionStatus.PENDING);
        return submissions.stream()
                .map(this::signSubmissionUrl)
                .collect(java.util.stream.Collectors.toList());
    }

    private Submission signSubmissionUrl(Submission sub) {
        if (sub.getSubmissionFileUrl() != null && !sub.getSubmissionFileUrl().isEmpty()) {
            // Only sign if it contains our bucket name, implying it's an S3 object we own
            if (sub.getSubmissionFileUrl().contains(bucketName)) {
                try {
                    // Generate presigned URL
                    String signedUrl = s3Service.generateSignedUrl(sub.getSubmissionFileUrl());

                    // Return a COPY with the signed URL to avoid dirty checking issues if attached
                    return Submission.builder()
                            .id(sub.getId())
                            .studentId(sub.getStudentId())
                            .task(sub.getTask())
                            .submissionFileUrl(signedUrl) // Use signed URL
                            .status(sub.getStatus())
                            .score(sub.getScore())
                            .feedback(sub.getFeedback())
                            .submittedAt(sub.getSubmittedAt())
                            .reviewedAt(sub.getReviewedAt())
                            .build();
                } catch (Exception e) {
                    log.error("Failed to sign URL for submission {}", sub.getId(), e);
                    // DEBUG: Expose error in the URL field so we can see it in UI
                    return Submission.builder()
                            .id(sub.getId())
                            .studentId(sub.getStudentId())
                            .task(sub.getTask())
                            .submissionFileUrl("ERROR: " + e.toString())
                            .status(sub.getStatus())
                            .score(sub.getScore())
                            .feedback(sub.getFeedback())
                            .submittedAt(sub.getSubmittedAt())
                            .reviewedAt(sub.getReviewedAt())
                            .build();
                }
            }
        }
        return sub;
    }

    @Transactional
    public void initializeTaskSchedules(Long studentId, String domain) {
        List<Task> domainTasks = taskRepository.findByDomainAndActiveTrue(domain);

        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate week1UnlockDate;

        // If today is Monday, Week 1 starts today. Otherwise, next Monday.
        if (now.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
            week1UnlockDate = now;
        } else {
            week1UnlockDate = now.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        }

        java.time.LocalDate finalWeek1UnlockDate = week1UnlockDate;

        List<TaskSchedule> schedules = domainTasks.stream()
                .filter(task -> !taskScheduleRepository.findByStudentIdAndTaskId(studentId, task.getId()).isPresent())
                .map(task -> {
                    java.time.LocalDate unlockDate;
                    java.time.LocalDateTime deadline;

                    if (task.isManual()) {
                        // Manual Mode: Use configured dates strictly
                        if (task.getStartDate() != null) {
                            unlockDate = java.time.LocalDate.ofInstant(task.getStartDate(), java.time.ZoneId.of("UTC"));
                        } else {
                            unlockDate = java.time.LocalDate.now(); // Fallback
                        }

                        if (task.getDeadline() != null) {
                            deadline = java.time.LocalDateTime.ofInstant(task.getDeadline(),
                                    java.time.ZoneId.of("UTC"));
                        } else {
                            deadline = unlockDate.plusDays(6).atTime(23, 59, 59);
                        }
                    } else {
                        // Auto Mode: Weekly Schedule relative to "Next Monday"
                        long weekOffset = task.getWeekNo() - 1;
                        unlockDate = finalWeek1UnlockDate.plusWeeks(weekOffset);
                        deadline = unlockDate.plusDays(6).atTime(23, 59, 59);
                    }

                    // Tasks are locked initially. Scheduler unlocks them when date arrives.
                    // Exception: If current date matches unlock date (e.g. today is Monday), we can
                    // unlock immediately?
                    // Better to let Scheduler handle it or do it here if date matches.
                    boolean shouldUnlock = !unlockDate.isAfter(now) && task.getWeekNo() == 1;
                    if (task.isManual() && !unlockDate.isAfter(now)) {
                        shouldUnlock = true;
                    }

                    return TaskSchedule.builder()
                            .studentId(studentId)
                            .task(task)
                            .unlockDate(unlockDate)
                            .deadline(deadline)
                            .isUnlocked(shouldUnlock)
                            .isSubmitted(false)
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        if (!schedules.isEmpty()) {
            taskScheduleRepository.saveAll(schedules);
            log.info("Initialized {} new task schedules for student {}", schedules.size(), studentId);
        } else {
            log.info("No new tasks to initialize for student {}", studentId);
        }
    }

    @Transactional
    public Submission reviewSubmission(Long submissionId, String status, Integer score) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Submission.SubmissionStatus newStatus = Submission.SubmissionStatus.valueOf(status);
        submission.setStatus(newStatus);
        submission.setScore(score);

        return submissionRepository.save(submission);
    }

    @Transactional
    public Task updateTask(Long taskId, com.skilledup.task.dto.CreateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDomain(request.getDomain());
        task.setWeekNo(request.getWeekNo());

        // Prevent overwriting with signed URLs (unless it's a new upload/link)
        // Check for common signature parameters (V4 and V2)
        if (request.getTaskFileUrl() != null && !isSignedUrl(request.getTaskFileUrl())) {
            task.setTaskFileUrl(request.getTaskFileUrl());
        } else if (request.getTaskFileUrl() == null) {
            task.setTaskFileUrl(null);
        }

        if (request.getVideoUrl() != null && !isSignedUrl(request.getVideoUrl())) {
            task.setVideoUrl(request.getVideoUrl());
        } else if (request.getVideoUrl() == null) {
            task.setVideoUrl(null);
        }

        if (request.getUrlFileUrl() != null && !isSignedUrl(request.getUrlFileUrl())) {
            task.setUrlFileUrl(request.getUrlFileUrl());
        } else if (request.getUrlFileUrl() == null) {
            task.setUrlFileUrl(null);
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }
        if (request.getIsManual() != null) {
            task.setManual(request.getIsManual());
        }
        if (request.getAutoReview() != null) {
            task.setAutoReview(request.getAutoReview());
        }
        if (request.getActive() != null) {
            task.setActive(request.getActive());
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Updated task {} with ID {}", updatedTask.getTitle(), taskId);
        return updatedTask;
    }

    private boolean isSignedUrl(String url) {
        if (url == null)
            return false;
        // Robust check for S3 signed URLs
        return url.contains("X-Amz-Signature") ||
                url.contains("Signature=") ||
                url.contains("Expires=") ||
                (url.contains("?") && (url.contains("amazonaws.com") || url.contains(bucketName)));
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Delete associated task schedules first
        taskScheduleRepository.deleteByTaskId(taskId);

        // Delete the task
        taskRepository.delete(task);
        log.info("Deleted task {} with ID {}", task.getTitle(), taskId);
    }

    @Transactional
    public void deleteTaskFile(Long taskId, String fileType) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        String fileUrl = null;
        switch (fileType) {
            case "taskFile":
                fileUrl = task.getTaskFileUrl();
                task.setTaskFileUrl(null);
                break;
            case "video":
                fileUrl = task.getVideoUrl();
                task.setVideoUrl(null);
                break;
            case "urlFile":
                fileUrl = task.getUrlFileUrl();
                task.setUrlFileUrl(null);
                break;
            default:
                throw new RuntimeException("Invalid file type: " + fileType);
        }

        // Delete from S3 if file exists
        if (fileUrl != null && !fileUrl.isEmpty()) {
            try {
                s3Service.deleteFile(fileUrl);
                log.info("Deleted file from S3: {}", fileUrl);
            } catch (Exception e) {
                log.error("Error deleting file from S3: {}", e.getMessage());
                // Continue with database update even if S3 deletion fails
            }
        }

        taskRepository.save(task);
        log.info("Deleted {} file from task ID {}", fileType, taskId);
    }

    @Transactional
    public void deleteSubmission(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Update task schedule to allow re-submission
        TaskSchedule schedule = taskScheduleRepository
                .findByStudentIdAndTaskId(submission.getStudentId(), submission.getTask().getId())
                .orElseThrow(() -> new RuntimeException("Task schedule not found"));

        if (schedule.isSubmitted()) {
            schedule.setSubmitted(false);
            taskScheduleRepository.save(schedule);
        }

        // Delete from S3
        if (submission.getSubmissionFileUrl() != null) {
            try {
                s3Service.deleteFile(submission.getSubmissionFileUrl());
            } catch (Exception e) {
                log.error("Failed to delete submission file from S3", e);
            }
        }

        submissionRepository.delete(submission);
        log.info("Deleted submission {} for student {}", submissionId, submission.getStudentId());
    }

    @Transactional
    public void simulateDelay(Long studentId, Long taskId) {
        TaskSchedule schedule = taskScheduleRepository.findByStudentIdAndTaskId(studentId, taskId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // Set dates to past
        java.time.LocalDate lastMonday = java.time.LocalDate.now().minusDays(8);
        java.time.LocalDateTime lastSunday = lastMonday.plusDays(6).atTime(23, 59, 59);

        schedule.setUnlockDate(lastMonday);
        schedule.setDeadline(lastSunday);
        schedule.setUnlocked(true);
        schedule.setDelayed(true); // Force delayed

        taskScheduleRepository.save(schedule);
        log.info("Simulated delay for student {} task {}", studentId, taskId);
    }

    /**
     * Get task completion statistics for a student in a domain
     * Used for LOR eligibility checking
     */
    public java.util.Map<String, Object> getTaskCompletionStats(Long studentId, String domain) {
        List<TaskSchedule> schedules = taskScheduleRepository.findByStudentId(studentId);

        // Filter by domain
        List<TaskSchedule> domainSchedules = schedules.stream()
                .filter(s -> s.getTask().getDomain().equals(domain))
                .collect(java.util.stream.Collectors.toList());

        int totalTasks = domainSchedules.size();
        int completedTasks = (int) domainSchedules.stream()
                .filter(TaskSchedule::isSubmitted)
                .count();

        int completionPercent = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
        boolean meetsMinimumRequirement = completionPercent >= 95;

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalTasks", totalTasks);
        stats.put("completedTasks", completedTasks);
        stats.put("completionPercent", completionPercent);
        stats.put("meetsMinimumRequirement", meetsMinimumRequirement);

        log.info("Task completion stats for student {} in domain {}: {}/{} ({}%)",
                studentId, domain, completedTasks, totalTasks, completionPercent);

        return stats;
    }
}
