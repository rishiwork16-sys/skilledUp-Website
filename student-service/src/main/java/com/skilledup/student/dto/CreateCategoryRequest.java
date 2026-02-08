package com.skilledup.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank(message = "Cover image is required")
    private String coverImage;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationWeeks;

    private String slug;

    private String tagline;
    private String level;
    private String skills;
    private String tools;
    private Integer maxSeats;
    private Boolean autoStart;
    private Boolean allowReEnrollment;
    private String priority;
    private Boolean autoCertificate;
    private Integer loiPercentage;
    private String internalNotes;
}
