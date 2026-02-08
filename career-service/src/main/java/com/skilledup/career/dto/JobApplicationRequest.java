package com.skilledup.career.dto;

import lombok.Data;

@Data
public class JobApplicationRequest {
    private Long jobId;
    private String fullName;
    private String email;
    private String phone;
    
    // Address
    private String city;
    private String state;
    private String pincode;

    // Preferences
    private String workMode;
    private String preferredLocation;

    // Experience
    private String currentCompany;
    private Double totalExperience;
    private Double relevantExperience;
    private String noticePeriod;
    
    // Links
    private String linkedInUrl;
    private String githubUrl;
    private String otherPortfolioUrl;
    
    private String additionalInformation;
}
