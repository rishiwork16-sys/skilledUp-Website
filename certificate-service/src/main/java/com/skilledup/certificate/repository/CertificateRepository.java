package com.skilledup.certificate.repository;

import com.skilledup.certificate.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByStudentIdAndInternshipTypeId(Long studentId, Long internshipTypeId);

    boolean existsByCertificateNumber(String certificateNumber);
}
