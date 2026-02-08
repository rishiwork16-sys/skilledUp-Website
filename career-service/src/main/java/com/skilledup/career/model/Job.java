package com.skilledup.career.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String type; // Full-time, Internship, etc.

    @Column(nullable = false)
    private String experience; // e.g., "0-1 years"

    @Column(nullable = false)
    private String salary; // e.g., "Not Disclosed"

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirements;

    private boolean active;

    @CreationTimestamp
    private LocalDateTime postedDate;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
