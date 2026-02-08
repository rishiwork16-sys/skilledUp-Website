package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "internship_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title; // e.g., "Web Development", "Data Science"

    @Column(unique = true)
    private String slug;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String coverImage;

    @Column(length = 500)
    private String tagline;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer durationWeeks; // Duration in weeks

    private String level; // BEGINNER, INTERMEDIATE, ADVANCED
    private String skills; // Comma separated
    private String tools; // Comma separated

    private Integer maxSeats;

    @Builder.Default
    private boolean autoStart = false;

    @Builder.Default
    private boolean allowReEnrollment = false;

    @Builder.Default
    private String priority = "NORMAL";

    @Builder.Default
    private boolean autoCertificate = true;

    private Integer loiPercentage;

    @Column(length = 1000)
    private String internalNotes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
