package com.skilledup.student.dto;

import com.skilledup.student.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private Student.StudentStatus status;
    private boolean blocked;
    private Instant createdAt;

    // Internship details
    private String currentDomain;
    private Integer completedTasks;
    private Integer totalTasks;
    private Double progressPercentage;

    // New: List of all enrollments
    private java.util.List<com.skilledup.student.model.InternshipEnrollment> enrollments;

    // Submissions fetched from Task Service
    private java.util.List<Object> submissions;
}
