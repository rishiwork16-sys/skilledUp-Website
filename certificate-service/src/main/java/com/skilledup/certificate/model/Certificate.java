package com.skilledup.certificate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long internshipTypeId;

    @Column(nullable = false)
    private String certificateUrl; // S3 URL

    @Column(nullable = false)
    private String certificateNumber; // Unique ID like CER-2024-001

    @Column(nullable = false)
    private LocalDate issuedDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
