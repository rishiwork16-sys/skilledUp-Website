package com.skilledup.task.controller;

import com.skilledup.task.model.ExtensionRequest;
import com.skilledup.task.service.ExtensionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/extensions")
@RequiredArgsConstructor
public class ExtensionController {

    private final ExtensionService extensionService;

    @PostMapping("/request")
    public ExtensionRequest createRequest(@RequestBody CreateExtensionRequestDto dto) {
        return extensionService.createRequest(dto.getStudentId(), dto.getTaskId(), dto.getReason(),
                dto.getRequestedDays());
    }

    @PostMapping("/{requestId}/review")
    public ExtensionRequest reviewRequest(@PathVariable Long requestId, @RequestBody ReviewExtensionDto dto) {
        return extensionService.reviewRequest(requestId, dto.isApproved());
    }

    @GetMapping("/pending")
    public List<ExtensionRequest> getPendingRequests() {
        return extensionService.getAllPendingRequests();
    }

    @GetMapping("/student/{studentId}")
    public List<ExtensionRequest> getStudentRequests(@PathVariable Long studentId) {
        return extensionService.getStudentRequests(studentId);
    }

    @Data
    static class CreateExtensionRequestDto {
        private Long studentId;
        private Long taskId;
        private String reason;
        private Integer requestedDays;
    }

    @Data
    static class ReviewExtensionDto {
        private boolean approved;
    }
}
