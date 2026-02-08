package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "internship_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // e.g., "Web Development", "AI/ML"

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer durationInWeeks;

    private Long stipendInPaise;

    @Column(nullable = false)
    private boolean active = true;
}
