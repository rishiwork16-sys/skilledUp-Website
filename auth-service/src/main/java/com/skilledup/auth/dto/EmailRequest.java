package com.skilledup.auth.dto;

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
    private String subject;
    private String body;
    private String type; // Using String instead of Enum to decouple
    private String attachmentUrl;

    // Dynamic data for templates
    private String name;
    private String otp;
    private String taskTitle;
    private String deadline;
}
