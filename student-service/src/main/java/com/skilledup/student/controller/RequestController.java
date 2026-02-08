package com.skilledup.student.controller;

import com.skilledup.student.dto.ReviewRequestDTO;
import com.skilledup.student.model.ExitRequest;
import com.skilledup.student.model.ExtensionRequest;
import com.skilledup.student.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    // Extension Requests
    @GetMapping("/extensions")
    public ResponseEntity<List<ExtensionRequest>> getAllExtensions() {
        return ResponseEntity.ok(requestService.getAllExtensions());
    }

    @GetMapping("/extensions/pending")
    public ResponseEntity<List<ExtensionRequest>> getPendingExtensions() {
        return ResponseEntity.ok(requestService.getPendingExtensions());
    }

    @PostMapping("/extensions/{id}/review")
    public ResponseEntity<ExtensionRequest> reviewExtension(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO review) {
        return ResponseEntity.ok(requestService.reviewExtension(id, review));
    }

    // Exit Requests
    @GetMapping("/exits")
    public ResponseEntity<List<ExitRequest>> getAllExits() {
        return ResponseEntity.ok(requestService.getAllExits());
    }

    @GetMapping("/exits/pending")
    public ResponseEntity<List<ExitRequest>> getPendingExits() {
        return ResponseEntity.ok(requestService.getPendingExits());
    }

    @PostMapping("/exits/{id}/review")
    public ResponseEntity<ExitRequest> reviewExit(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO review) {
        return ResponseEntity.ok(requestService.reviewExit(id, review));
    }
}
