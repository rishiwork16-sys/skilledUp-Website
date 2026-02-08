package com.skilledup.student.dto;

import com.skilledup.student.model.InternshipEnrollment;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrollmentResponse {
    private InternshipEnrollment enrollment;
    private String token; // JWT Token for auto-login
}
