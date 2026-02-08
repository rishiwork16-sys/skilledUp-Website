package com.skilledup.student.service;

import com.skilledup.student.dto.EnrollmentRequest;
import com.skilledup.student.model.InternshipEnrollment;

import com.skilledup.student.dto.EnrollmentResponse;

public interface InternshipEnrollmentService {
    EnrollmentResponse enrollPublic(EnrollmentRequest request);

    // New method for manual regeneration
    String regenerateOfferLetter(Long enrollmentId);
}
