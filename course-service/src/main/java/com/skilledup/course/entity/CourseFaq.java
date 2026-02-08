package com.skilledup.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseFaq {
    @Column(length = 500)
    private String question;

    @Column(length = 4000)
    private String answer;
}

