package com.skilledup.student.controller;

import com.skilledup.student.dto.EnrollmentRequest;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.service.InternshipEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/enrollments")
@RequiredArgsConstructor
public class InternshipEnrollmentController {

    private final InternshipEnrollmentService enrollmentService;
    private final com.skilledup.student.repository.InternshipTypeRepository internshipTypeRepository;

    @PostMapping("/public")
    public ResponseEntity<com.skilledup.student.dto.EnrollmentResponse> enrollPublic(@RequestBody EnrollmentRequest request) {
        com.skilledup.student.dto.EnrollmentResponse response = enrollmentService.enrollPublic(request);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/types")
    public ResponseEntity<java.util.List<com.skilledup.student.model.InternshipType>> getActiveInternshipTypes() {
        // Assuming findAll for now, or add functionality to filter active
        return ResponseEntity.ok(internshipTypeRepository.findAll());
    }
}
