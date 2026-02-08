package com.skilledup.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String errorMessage;

    @Column(nullable = false)
    private Instant sentAt = Instant.now();

    public enum NotificationType {
        OTP, WELCOME, TASK_UNLOCK, DEADLINE_ALERT, CERTIFICATE_ISSUED, OFFER_LETTER, EXIT_LETTER
    }

    public enum NotificationStatus {
        SENT, FAILED
    }
}
