package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "placement_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String phoneNumber;
    private String appliedRole;
    private String attendedInterview;

    private String overallExperience;
    private String preparationLevel;

    @Column(length = 1000)
    private String improvementAreas;

    private String wantGuidance;

    @ElementCollection
    private List<String> supportNeeded;

    private String interestedInProgram;
    private String programType;
    private String budgetRange;

    @Column(length = 1000)
    private String additionalSupport;
    private String recommendSkilledUp;

    @ElementCollection
    private List<String> referralFor;

    private String referredName;
    private String referredEmail;
    private String referredPhone;
    private String referredRole;

    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
