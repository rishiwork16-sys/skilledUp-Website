package com.skilledup.notification.dto;

import com.skilledup.notification.model.NotificationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {

    private String recipient;
    private String subject; // Optional, can be derived from type
    private String body; // Optional, can be derived from type and data
    private NotificationLog.NotificationType type;
    private String attachmentUrl; // Optional S3 URL

    // Dynamic data for templates
    private String name;
    private String otp;
    private String taskTitle;
    private String deadline;
}
