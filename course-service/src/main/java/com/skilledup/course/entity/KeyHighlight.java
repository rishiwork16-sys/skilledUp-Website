package com.skilledup.course.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "course")
@EqualsAndHashCode(exclude = "course")
public class KeyHighlight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ElementCollection
    @CollectionTable(name = "key_highlight_items", joinColumns = @JoinColumn(name = "key_highlight_id"))
    @Column(name = "item", length = 1000)
    private List<String> items = new ArrayList<>();

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
}
