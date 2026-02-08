package com.skilledup.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String domain; // e.g., "Web Development", "AI/ML"

    @Column(nullable = false)
    private Integer weekNo; // Week 1, Week 2, etc.

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String taskFileUrl; // S3 URL for task PDF

    @Column(columnDefinition = "TEXT")
    private String videoUrl; // S3 URL for task video

    @Column(columnDefinition = "TEXT")
    private String urlFileUrl; // S3 URL for URL-uploaded file

    private Instant startDate; // Task start date (Manual mode)
    private Instant deadline; // Task deadline

    @Column(nullable = false)
    @Builder.Default
    private boolean isManual = false; // Manual scheduling mode

    @Column(nullable = false)
    @Builder.Default
    private boolean autoReview = false; // Auto-approve submissions

    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
