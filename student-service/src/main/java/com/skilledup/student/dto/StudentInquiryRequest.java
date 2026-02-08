package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInquiryRequest {
    private String fullName;
    private String email;
    private String mobileNumber;
    private String city;
    private String stateName;
    private String pinCode;
    private String backgroundName;
    private Long courseId;
    private String courseTitle;
    private String pagePath;
}

