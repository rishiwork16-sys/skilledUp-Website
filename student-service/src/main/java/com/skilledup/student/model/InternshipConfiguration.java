package com.skilledup.student.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "internship_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auto_start_batches")
    @Builder.Default
    private Boolean autoStartBatches = true;

    @Column(name = "start_day")
    @Builder.Default
    private String startDay = "MONDAY";

    @Column(name = "frequency")
    @Builder.Default
    private String frequency = "WEEKLY";

    @Column(name = "offer_letter_subject")
    @Builder.Default
    private String offerLetterSubject = "Internship Offer Letter - SkilledUp";

    @Lob
    @Column(name = "offer_letter_body", columnDefinition = "TEXT")
    @Builder.Default
    private String offerLetterBody = "Dear {{studentName}},\n\nWe are pleased to offer you an internship position as a {{domain}} Intern at SkilledUp.\nYour internship is scheduled to start on {{startDate}} and will continue for {{duration}} weeks.\n\nRegards,\nThe SkilledUp Team";

    @Column(name = "offer_letter_template_key", length = 2000)
    private String offerLetterTemplateKey;

    @Column(name = "offer_letter_template_updated_at")
    private Instant offerLetterTemplateUpdatedAt;
}
