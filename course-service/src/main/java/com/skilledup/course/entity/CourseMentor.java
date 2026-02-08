package com.skilledup.course.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseMentor {
    @Column(length = 255)
    private String name;

    @Column(length = 500)
    private String role;

    private Integer workExpYears;
    private Integer teachExpYears;

    @Column(length = 2000)
    private String about;

    @Column(length = 2000)
    private String imageKey;

    @Transient
    private String imageUrl;
}

