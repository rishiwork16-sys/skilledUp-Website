package com.skilledup.career.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
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
    private String portfolioUrl;
    private String githubUrl;
    private String otherPortfolioUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(nullable = false)
    private String resumeKey; // S3 Key

    @CreationTimestamp
    private LocalDateTime appliedDate;
}
