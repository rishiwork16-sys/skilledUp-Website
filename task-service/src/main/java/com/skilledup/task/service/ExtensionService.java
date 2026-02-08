package com.skilledup.task.service;

import com.skilledup.task.client.NotificationClient;
import com.skilledup.task.dto.EmailRequest;
import com.skilledup.task.model.ExtensionRequest;
import com.skilledup.task.model.TaskSchedule;
import com.skilledup.task.repository.ExtensionRequestRepository;
import com.skilledup.task.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtensionService {

    private final ExtensionRequestRepository extensionRequestRepository;
    private final TaskScheduleRepository taskScheduleRepository;
    private final NotificationClient notificationClient;

    @Transactional
    public ExtensionRequest createRequest(Long studentId, Long taskId, String reason, Integer requestedDays) {
        // limit logic could go here (e.g., max 1 active request per task)
        ExtensionRequest request = ExtensionRequest.builder()
                .studentId(studentId)
                .taskId(taskId)
                .reason(reason)
                .requestedDays(requestedDays)
                .status(ExtensionRequest.ExtensionStatus.PENDING)
                .build();

        log.info("Student {} requested extension for task {}", studentId, taskId);
        return extensionRequestRepository.save(request);
    }

    @Transactional
    public ExtensionRequest reviewRequest(Long requestId, boolean approved) {
        ExtensionRequest request = extensionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Extension request not found"));

        if (request.getStatus() != ExtensionRequest.ExtensionStatus.PENDING) {
            throw new RuntimeException("Request is already processed");
        }

        request.setReviewedAt(LocalDateTime.now());

        if (approved) {
            request.setStatus(ExtensionRequest.ExtensionStatus.APPROVED);

            // Apply extension to TaskSchedule
            TaskSchedule schedule = taskScheduleRepository
                    .findByStudentIdAndTaskId(request.getStudentId(), request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task Schedule not found for this request"));

            // Extend deadline
            LocalDateTime newDeadline = schedule.getDeadline().plusDays(request.getRequestedDays());
            schedule.setDeadline(newDeadline);

            // Reset Delayed status if effective
            if (newDeadline.isAfter(LocalDateTime.now())) {
                schedule.setDelayed(false);
            }

            taskScheduleRepository.save(schedule);
            log.info("Approved extension for Request ID {}. New Deadline: {}", requestId, newDeadline);

            // Send Email
            try {
                // notificationClient.sendEmail(...) // Placeholder
                log.info("Sending Extension APPROVED email to Student {}", request.getStudentId());
            } catch (Exception e) {
                log.error("Failed to send extension approval email", e);
            }

        } else {
            request.setStatus(ExtensionRequest.ExtensionStatus.REJECTED);
            log.info("Rejected extension for Request ID {}", requestId);
            // Send Email
            try {
                // notificationClient.sendEmail(...) // Placeholder
                log.info("Sending Extension REJECTED email to Student {}", request.getStudentId());
            } catch (Exception e) {
                log.error("Failed to send extension rejection email", e);
            }
        }

        return extensionRequestRepository.save(request);
    }

    public List<ExtensionRequest> getAllPendingRequests() {
        return extensionRequestRepository.findByStatus(ExtensionRequest.ExtensionStatus.PENDING);
    }

    public List<ExtensionRequest> getStudentRequests(Long studentId) {
        return extensionRequestRepository.findByStudentId(studentId);
    }
}
