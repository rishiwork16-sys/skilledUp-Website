package com.skilledup.notification.service;

import com.skilledup.notification.dto.EmailRequest;
import com.skilledup.notification.model.NotificationLog;
import com.skilledup.notification.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Async
    public void sendEmail(EmailRequest request) {
        log.info("Sending {} email to {}", request.getType(), request.getRecipient());

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(request.getRecipient());

            String subject = getSubject(request);
            String body = getBody(request);

            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isEmpty()) {
                // In a real scenario, we'd download the file from S3 and attach it
                // For now, we'll just include the link in the body
                body += "<br><br><a href='" + request.getAttachmentUrl() + "'>Download Attachment</a>";
                helper.setText(body, true);
            }

            javaMailSender.send(message);

            // Log success
            saveLog(request.getRecipient(), subject, body, request.getType(), NotificationLog.NotificationStatus.SENT,
                    null);
            log.info("Email sent successfully to {}", request.getRecipient());

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            saveLog(request.getRecipient(), request.getSubject(), "Error", request.getType(),
                    NotificationLog.NotificationStatus.FAILED, e.getMessage());
        }
    }

    private String getSubject(EmailRequest request) {
        if (request.getSubject() != null)
            return request.getSubject();

        return switch (request.getType()) {
            case OTP -> "Your Skilledup OTP Verification Code";
            case WELCOME -> "Welcome to Skilledup Internship Platform!";
            case TASK_UNLOCK -> "New Task Unlocked: " + request.getTaskTitle();
            case DEADLINE_ALERT -> "Urgent: Task Deadline Approaching!";
            case CERTIFICATE_ISSUED -> "Congratulations! Your Certificate is Ready";
            case OFFER_LETTER -> "Your Internship Offer Letter";
            case EXIT_LETTER -> "Internship Exit Letter";
            default -> "Notification from Skilledup";
        };
    }

    private String getBody(EmailRequest request) {
        if (request.getBody() != null)
            return request.getBody();

        // Simple HTML templates (In production, use Thymeleaf)
        String template = "";

        switch (request.getType()) {
            case OTP:
                template = "<h3>OTP Verification</h3><p>Your OTP code is: <b>" + request.getOtp()
                        + "</b></p><p>This code expires in 5 minutes.</p>";
                break;
            case WELCOME:
                template = "<h3>Welcome " + request.getName()
                        + "!</h3><p>We are excited to have you on board. Please complete your profile to start.</p>";
                break;
            case TASK_UNLOCK:
                template = "<h3>New Task Unlocked</h3><p>Week " + request.getTaskTitle()
                        + " is now available.</p><p>Good luck!</p>";
                break;
            case DEADLINE_ALERT:
                template = "<h3>Deadline Alert</h3><p>The deadline for " + request.getTaskTitle() + " is "
                        + request.getDeadline() + ". Please submit soon.</p>";
                break;
            case CERTIFICATE_ISSUED:
                template = "<h3>Certificate Issued</h3><p>Congratulations! You have completed your internship.</p>";
                break;
            default:
                template = "<p>You have a new notification.</p>";
        }

        return template;
    }

    private void saveLog(String recipient, String subject, String body, NotificationLog.NotificationType type,
            NotificationLog.NotificationStatus status, String error) {
        NotificationLog log = NotificationLog.builder()
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .type(type)
                .status(status)
                .errorMessage(error)
                .sentAt(Instant.now())
                .build();

        notificationLogRepository.save(log);
    }
}
