package com.skilledup.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequestDTO {
    @NotNull(message = "Approval status is required")
    private Boolean approved;

    @NotBlank(message = "Admin response is required")
    private String adminResponse;
}
