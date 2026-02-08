package com.skilledup.student.dto;

import lombok.Data;

@Data
public class ExitRequest {
    private Long enrollmentId;
    private String reason;
    private String lastWorkingDay; // Changed to String to avoid serialization issues
    private String feedback;
}
