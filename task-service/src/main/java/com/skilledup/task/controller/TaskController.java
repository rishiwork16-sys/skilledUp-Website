package com.skilledup.task.controller;

import com.skilledup.task.dto.ApiMessage;
import com.skilledup.task.dto.SubmissionRequest;
import com.skilledup.task.model.Submission;
import com.skilledup.task.model.TaskSchedule;
import com.skilledup.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final com.skilledup.task.scheduler.TaskReminderScheduler taskReminderScheduler;

    @PostMapping("/test-reminder")
    public ResponseEntity<String> triggerReminder() {
        taskReminderScheduler.sendTaskOverdueReminders();
        return ResponseEntity.ok("Reminder Scheduler Triggered Manually");
    }

    @PostMapping
    public ResponseEntity<?> createTask(
            @Valid @RequestBody com.skilledup.task.dto.CreateTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.createTask(request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeTasks(
            @RequestParam Long studentId,
            @RequestParam String domain) {
        taskService.initializeTaskSchedules(studentId, domain);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskSchedule>> getMyTasks(@RequestParam Long studentId) {
        return ResponseEntity.ok(taskService.getMyTasks(studentId));
    }

    @GetMapping
    public ResponseEntity<List<com.skilledup.task.model.Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.skilledup.task.model.Task> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/submit")
    public ResponseEntity<Submission> submitTask(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(taskService.submitTask(request));
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<List<Submission>> getMySubmissions(@RequestParam Long studentId) {
        return ResponseEntity.ok(taskService.getMySubmissions(studentId));
    }

    @GetMapping("/submissions/pending")
    public ResponseEntity<List<Submission>> getPendingSubmissions() {
        return ResponseEntity.ok(taskService.getPendingSubmissions());
    }

    @PostMapping("/submissions/{id}/review")
    public ResponseEntity<Submission> reviewSubmission(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam Integer score) {
        return ResponseEntity.ok(taskService.reviewSubmission(id, status, score));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Task Service is running"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.skilledup.task.model.Task> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody com.skilledup.task.dto.CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/submissions/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        taskService.deleteSubmission(id);
        return ResponseEntity.ok().build();
    }

    // Verification Helper
    @PostMapping("/simulate-delay")
    public ResponseEntity<Void> simulateDelay(@RequestParam Long studentId, @RequestParam Long taskId) {
        taskService.simulateDelay(studentId, taskId);
        return ResponseEntity.ok().build();
    }
}
