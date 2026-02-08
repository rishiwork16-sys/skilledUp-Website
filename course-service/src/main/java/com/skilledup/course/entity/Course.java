package com.skilledup.course.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"modules", "keyHighlights"})
@EqualsAndHashCode(exclude = {"modules", "keyHighlights"})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private String slug;

    private Double price;
    private Double originalPrice;
    private Integer discount;

    private String duration;
    private String mode;
    private String thumbnailUrl;
    private String certificateUrl;
    private String introVideoUrl;
    private String category;

    @JsonManagedReference
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Module> modules = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KeyHighlight> keyHighlights = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CareerOpportunity> careerOpportunities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_skills", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_faqs", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "sort_order")
    private List<CourseFaq> faqs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_tools_covered", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "tool_image_url", length = 2000)
    private List<String> toolsCovered = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_mentors", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "sort_order")
    private List<CourseMentor> mentors = new ArrayList<>();
}
