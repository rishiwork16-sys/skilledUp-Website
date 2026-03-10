package com.skilledup.student.controller;

import com.skilledup.student.model.PlacementFeedback;
import com.skilledup.student.service.PlacementFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PlacementFeedbackController {

    private final PlacementFeedbackService feedbackService;

    @PostMapping("/api/feedbacks")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody PlacementFeedback feedback) {
        PlacementFeedback saved = feedbackService.submitFeedback(feedback);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "message", "Feedback submitted successfully"));
    }

    @GetMapping("/api/admin/feedbacks")
    public ResponseEntity<List<PlacementFeedback>> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }
}
