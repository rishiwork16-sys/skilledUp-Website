package com.skilledup.student.controller;

import com.skilledup.student.dto.LORRequestDTO;
import com.skilledup.student.model.LORRequest;
import com.skilledup.student.service.LORService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lor")
@RequiredArgsConstructor
public class LORController {

    private final LORService lorService;

    /**
     * Check eligibility for LOR
     */
    @GetMapping("/check-eligibility")
    public ResponseEntity<LORService.EligibilityResult> checkEligibility(
            @RequestParam(required = false) Long studentId,
            @RequestParam Long enrollmentId) {
        return ResponseEntity.ok(lorService.checkEligibility(studentId, enrollmentId));
    }

    /**
     * Student requests LOR (Auto-Approved if Eligible)
     */
    @PostMapping("/request")
    public ResponseEntity<LORRequest> requestLOR(
            @RequestParam Long studentId,
            @Valid @RequestBody LORRequestDTO requestDTO) {
        return ResponseEntity.ok(lorService.requestLOR(studentId, requestDTO));
    }

    /**
     * Get all LOR requests for a student
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<LORRequest>> getMyRequests(@RequestParam Long studentId) {
        return ResponseEntity.ok(lorService.getMyRequests(studentId));
    }

    /**
     * Get LOR request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LORRequest> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(lorService.getRequestById(id));
    }

    /**
     * Download LOR (returns URL)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<String> downloadLOR(@PathVariable Long id) {
        LORRequest request = lorService.getRequestById(id);

        if (request.getLorUrl() == null || request.getLorUrl().isEmpty()) {
            return ResponseEntity.badRequest().body("LOR not yet generated");
        }

        return ResponseEntity.ok(request.getLorUrl());
    }

    @DeleteMapping("/request/reset")
    public ResponseEntity<String> resetLORRequest(
            @RequestParam Long studentId,
            @RequestParam Long enrollmentId) {
        lorService.resetLOR(studentId, enrollmentId);
        return ResponseEntity.ok("LOR Request Reset Successfully");
    }
}
