package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

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
    private LocalDate currentEndDate;

    @Column(nullable = false)
    private LocalDate requestedEndDate;

    @Column(length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(length = 2000)
    private String adminResponse;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant reviewedAt;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
