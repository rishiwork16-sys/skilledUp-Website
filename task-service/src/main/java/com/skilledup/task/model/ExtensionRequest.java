package com.skilledup.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "extension_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private Integer requestedDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public enum ExtensionStatus {
        PENDING, APPROVED, REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ExtensionStatus.PENDING;
        }
    }
}
