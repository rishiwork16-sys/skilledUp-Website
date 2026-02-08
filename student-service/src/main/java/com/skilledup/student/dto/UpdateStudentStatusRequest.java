package com.skilledup.student.dto;

import com.skilledup.student.model.Student;
import lombok.Data;

@Data
public class UpdateStudentStatusRequest {
    private Student.StudentStatus status;
}
