    package com.skilledup.task.dto;

import lombok.Data;

@Data
public class CreateTaskRequest {
    private String title;
    private String description;
    private String domain;
    private Integer weekNo;
    private String taskFileUrl;
    private String videoUrl;
    private String urlFileUrl;
    private Boolean autoReview;
    private Boolean isManual;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private java.time.Instant startDate;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private java.time.Instant deadline;

    private Boolean active;
}
