package com.skilledup.student.service;

import com.skilledup.student.dto.ReviewRequestDTO;
import com.skilledup.student.model.ExitRequest;
import com.skilledup.student.model.ExtensionRequest;
import com.skilledup.student.repository.ExitRequestRepository;
import com.skilledup.student.repository.ExtensionRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final ExtensionRequestRepository extensionRequestRepository;
    private final ExitRequestRepository exitRequestRepository;

    // Extension Requests
    public List<ExtensionRequest> getPendingExtensions() {
        return extensionRequestRepository.findByStatus(ExtensionRequest.RequestStatus.PENDING);
    }

    public List<ExtensionRequest> getAllExtensions() {
        return extensionRequestRepository.findAll();
    }

    @Transactional
    public ExtensionRequest reviewExtension(Long id, ReviewRequestDTO review) {
        ExtensionRequest request = extensionRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Extension request not found"));

        request.setStatus(review.getApproved() ? ExtensionRequest.RequestStatus.APPROVED
                : ExtensionRequest.RequestStatus.REJECTED);
        request.setAdminResponse(review.getAdminResponse());
        request.setReviewedAt(Instant.now());

        ExtensionRequest updated = extensionRequestRepository.save(request);
        log.info("Reviewed extension request {}: {}", id, request.getStatus());
        return updated;
    }

    // Exit Requests
    public List<ExitRequest> getPendingExits() {
        return exitRequestRepository.findByStatus(ExitRequest.RequestStatus.PENDING);
    }

    public List<ExitRequest> getAllExits() {
        return exitRequestRepository.findAll();
    }

    @Transactional
    public ExitRequest reviewExit(Long id, ReviewRequestDTO review) {
        ExitRequest request = exitRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exit request not found"));

        request.setStatus(
                review.getApproved() ? ExitRequest.RequestStatus.APPROVED : ExitRequest.RequestStatus.REJECTED);
        request.setAdminResponse(review.getAdminResponse());
        request.setReviewedAt(Instant.now());

        ExitRequest updated = exitRequestRepository.save(request);
        log.info("Reviewed exit request {}: {}", id, request.getStatus());
        return updated;
    }
}
