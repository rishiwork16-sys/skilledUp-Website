package com.skilledup.certificate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "certificate_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internship_certificate_template_key", length = 2000)
    private String internshipCertificateTemplateKey;

    @Column(name = "internship_certificate_template_updated_at")
    private Instant internshipCertificateTemplateUpdatedAt;

    @Column(name = "lor_template_key", length = 2000)
    private String lorTemplateKey;

    @Column(name = "lor_template_updated_at")
    private Instant lorTemplateUpdatedAt;
}
