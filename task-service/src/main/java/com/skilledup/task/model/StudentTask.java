package com.skilledup.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_tasks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "task_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    private String submissionUrl; // S3 Link or Text

    private String remarks;
    
    private Integer score; // 0-100

    private Instant submittedAt;

    private Instant gradedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum TaskStatus {
        PENDING, SUBMITTED, APPROVED, REJECTED
    }
}
