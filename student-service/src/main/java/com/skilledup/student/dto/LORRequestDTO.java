package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LORRequestDTO {
    private Long enrollmentId;
    private String purpose; // Optional: Job Application, Higher Studies, etc.
}
