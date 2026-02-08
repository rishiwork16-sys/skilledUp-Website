package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LORGenerationRequest {
    private String studentName;
    private String email;
    private String domain;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration; // weeks
    private Double completionPercent;
    private String lorId; // Unique ID like LOR-2025-XXXX
}
