package com.skilledup.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // Reference to Student Service

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private String submissionFileUrl; // S3 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    private Integer score; // 0-100

    @Column(length = 1000)
    private String feedback;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    private Instant reviewedAt;

    public enum SubmissionStatus {
        PENDING, APPROVED, REJECTED, DELAYED
    }
}
