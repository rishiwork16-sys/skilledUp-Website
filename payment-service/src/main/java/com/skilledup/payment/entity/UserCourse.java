package com.skilledup.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_courses")
public class UserCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "access_status")
    private String accessStatus; // ACTIVE, EXPIRED, REVOKED

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
        if (accessStatus == null) {
            accessStatus = "ACTIVE";
        }
    }
}
