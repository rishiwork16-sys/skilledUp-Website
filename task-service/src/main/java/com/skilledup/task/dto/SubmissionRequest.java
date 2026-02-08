package com.skilledup.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Submission file URL is required")
    private String submissionFileUrl; // S3 URL after upload
}
