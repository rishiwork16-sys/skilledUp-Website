package com.skilledup.career.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationAdminResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;

    private String fullName;
    private String email;
    private String phone;
    private String currentCompany;

    private String city;
    private String state;
    private String pincode;

    private String workMode;
    private String preferredLocation;

    private Double totalExperience;
    private Double relevantExperience;
    private String noticePeriod;

    private LocalDateTime appliedDate;
}

