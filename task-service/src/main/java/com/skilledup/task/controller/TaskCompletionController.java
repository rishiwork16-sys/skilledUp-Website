package com.skilledup.task.controller;

import com.skilledup.task.dto.ApiMessage;
import com.skilledup.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskCompletionController {

    private final TaskService taskService;

    /**
     * Get task completion statistics for a student in a specific domain
     * Used by LOR eligibility checking
     */
    @GetMapping("/completion-stats")
    public ResponseEntity<Map<String, Object>> getTaskCompletionStats(
            @RequestParam Long studentId,
            @RequestParam String domain) {

        Map<String, Object> stats = taskService.getTaskCompletionStats(studentId, domain);
        return ResponseEntity.ok(stats);
    }
}
