package com.skilledup.student.client;

import com.skilledup.student.dto.ApiMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "task-service", url = "${application.config.task-service-url:http://localhost:8084}")
public interface TaskClient {

    // Helper DTOs for Task Service responses might be needed in student-service
    // For now returning Object or creating DTOs in student-service

    @GetMapping("/api/tasks/my-tasks")
    Object getMyTasks(@RequestParam("studentId") Long studentId);

    @PostMapping("/api/tasks/initialize")
    void initializeTasks(@RequestParam("studentId") Long studentId, @RequestParam("domain") String domain);

    @PostMapping("/api/tasks/submit")
    Object submitTask(@RequestBody Object submissionRequest);

    @GetMapping("/api/tasks/my-submissions")
    List<Object> getMySubmissions(@RequestParam("studentId") Long studentId);

    @GetMapping("/api/tasks/completion-stats")
    com.skilledup.student.dto.TaskCompletionStatsDTO getTaskCompletionStats(
            @RequestParam("studentId") Long studentId,
            @RequestParam("domain") String domain);
}
