package com.skilledup.certificate.repository;

import com.skilledup.certificate.model.CertificateConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateConfigurationRepository extends JpaRepository<CertificateConfiguration, Long> {
}

