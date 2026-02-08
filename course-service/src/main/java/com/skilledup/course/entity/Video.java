package com.skilledup.course.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "module")
@EqualsAndHashCode(exclude = "module")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private String s3Key; // Key in S3 bucket
    private String duration;

    private Boolean isLocked; // Example: false can be previewed

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;
}
