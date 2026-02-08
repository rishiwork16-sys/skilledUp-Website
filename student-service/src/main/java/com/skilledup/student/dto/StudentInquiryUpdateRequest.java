package com.skilledup.student.dto;

import com.skilledup.student.model.StudentInquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInquiryUpdateRequest {
    private StudentInquiry.InquiryStatus status;
    private String adminNotes;
}

