package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_inquiries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 20)
    private String mobileNumber;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String stateName;

    @Column(nullable = false, length = 10)
    private String pinCode;

    @Column(nullable = false, length = 200)
    private String backgroundName;

    private Long courseId;

    @Column(length = 300)
    private String courseTitle;

    @Column(length = 300)
    private String pagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum InquiryStatus {
        NEW, CONTACTED, CLOSED
    }
}

