package com.skilledup.certificate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "loi_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoiRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String loiUrl; // S3 URL

    private Integer performanceScore; // Must be >= 95

    @Column(nullable = false)
    private LocalDate issuedDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean emailed = false;
}
