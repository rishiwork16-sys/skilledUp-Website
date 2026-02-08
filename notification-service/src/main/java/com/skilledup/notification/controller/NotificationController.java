package com.skilledup.notification.controller;

import com.skilledup.notification.dto.ApiMessage;
import com.skilledup.notification.dto.EmailRequest;
import com.skilledup.notification.service.EmailService;
import com.skilledup.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;
    private final com.skilledup.notification.service.SmsService smsService;

    @PostMapping("/send")
    public ResponseEntity<ApiMessage> sendEmail(@RequestBody EmailRequest request) {
        emailService.sendEmail(request);
        return ResponseEntity.ok(new ApiMessage("Email request accepted"));
    }

    @PostMapping("/send-sms")
    public ResponseEntity<ApiMessage> sendSms(@RequestBody com.skilledup.notification.dto.SmsRequest request) {
        boolean success = smsService.sendOtp(request.getMobile(), request.getOtp());
        if (success) {
            return ResponseEntity.ok(new ApiMessage("SMS sent successfully"));
        } else {
            return ResponseEntity.status(500).body(new ApiMessage("Failed to send SMS"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Notification Service is running"));
    }

    @GetMapping("/logs/last")
    public ResponseEntity<java.util.List<com.skilledup.notification.model.NotificationLog>> getLastLogs() {
        // This is a temporary debug endpoint
        return ResponseEntity.ok(notificationLogRepository.findTop5ByOrderBySentAtDesc());
    }

    private final com.skilledup.notification.repository.NotificationLogRepository notificationLogRepository;
}
