package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "internship_program_enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "internship_category_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "internship_category_id", nullable = false)
    private InternshipCategory internshipCategory;

    private String offerLetterUrl; // S3 URL

    private String certificateUrl; // Certificate S3/Local URL

    @Column(nullable = false)
    private Integer duration; // in weeks

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(nullable = false)
    private Integer progress = 0; // percentage 0-100

    @Column(nullable = false)
    @Builder.Default
    private Instant enrolledAt = Instant.now();

    public enum EnrollmentStatus {
        ACTIVE, COMPLETED, DELAYED, TERMINATED
    }
}
