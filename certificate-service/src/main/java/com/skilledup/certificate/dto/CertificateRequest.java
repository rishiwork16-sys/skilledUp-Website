package com.skilledup.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {
    private Long studentId;
    private Long internshipTypeId;
    private String studentName;
    private String internshipTitle; // e.g., "Web Development"
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer performanceScore; // Optional, required for LOI logic
}
