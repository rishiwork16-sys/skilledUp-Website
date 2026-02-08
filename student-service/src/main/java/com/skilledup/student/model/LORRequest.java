package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "lor_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "enrollment_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LORRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long enrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @Column(nullable = false)
    private Boolean eligibilityChecked = false;

    private Integer taskCompletionPercent;

    private Integer totalTasks;

    private Integer completedTasks;

    @Column(length = 500)
    private String lorUrl; // S3 URL after generation

    @Column(unique = true, length = 50)
    private String uniqueLorId; // e.g., LOR-2025-00001

    @Column(columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    private Instant approvedAt;

    private Instant rejectedAt;

    private Instant generatedAt;

    public enum RequestStatus {
        PENDING, // Student requested, waiting for admin review
        APPROVED, // Admin approved, waiting for generation
        REJECTED, // Admin rejected
        GENERATED // LOR PDF generated and available
    }
}
