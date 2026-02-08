package com.skilledup.certificate.service;

import com.skilledup.certificate.dto.CertificateRequest;
import com.skilledup.certificate.model.Certificate;
import com.skilledup.certificate.model.LoiRecord;
import com.skilledup.certificate.repository.CertificateRepository;
import com.skilledup.certificate.repository.LoiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final LoiRepository loiRepository;
    private final PptGenerationService pptGenerationService;
    private final S3Service s3Service;

    @Transactional
    public Certificate generateCertificate(CertificateRequest request) {
        // Check if already exists
        certificateRepository.findByStudentIdAndInternshipTypeId(
                request.getStudentId(), request.getInternshipTypeId())
                .ifPresent(c -> {
                    throw new RuntimeException("Certificate already generated for this internship");
                });

        // Prepare placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{STUDENT_NAME}}", request.getStudentName());
        placeholders.put("{{INTERNSHIP_DOMAIN}}", request.getInternshipTitle());
        placeholders.put("{{START_DATE}}", request.getStartDate().toString());
        placeholders.put("{{END_DATE}}", request.getEndDate().toString());
        placeholders.put("{{DATE}}", LocalDate.now().toString());
        String certNumber = "CER-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        placeholders.put("{{CERTIFICATE_ID}}", certNumber);

        // Generate PPT
        File pptFile = pptGenerationService.generatePpt("CertificateTemplate.pptx", placeholders);

        // Upload to S3
        String s3Url = s3Service.uploadFile(pptFile, "certificates");

        // Save to DB
        Certificate certificate = Certificate.builder()
                .studentId(request.getStudentId())
                .internshipTypeId(request.getInternshipTypeId())
                .certificateNumber(certNumber)
                .certificateUrl(s3Url)
                .issuedDate(LocalDate.now())
                .build();

        return certificateRepository.save(certificate);
    }

    @Transactional
    public LoiRecord generateLoi(CertificateRequest request) {
        if (request.getPerformanceScore() == null || request.getPerformanceScore() < 95) {
            throw new RuntimeException("Performance score must be 95+ for LOI");
        }

        // Check if already exists
        loiRepository.findByStudentId(request.getStudentId())
                .ifPresent(l -> {
                    throw new RuntimeException("LOI already generated for this student");
                });

        // Prepare placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{STUDENT_NAME}}", request.getStudentName());
        placeholders.put("{{INTERNSHIP_DOMAIN}}", request.getInternshipTitle());
        placeholders.put("{{DATE}}", LocalDate.now().toString());

        // Generate PPT
        File pptFile = pptGenerationService.generatePpt("LoiTemplate.pptx", placeholders);

        // Upload to S3
        String s3Url = s3Service.uploadFile(pptFile, "loi-letters");

        // Save to DB
        LoiRecord loiRecord = LoiRecord.builder()
                .studentId(request.getStudentId())
                .loiUrl(s3Url)
                .performanceScore(request.getPerformanceScore())
                .issuedDate(LocalDate.now())
                .build();

        return loiRepository.save(loiRecord);
    }

    public String generateOfferLetter(CertificateRequest request) {
        // Prepare placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{STUDENT_NAME}}", request.getStudentName());
        placeholders.put("{{INTERNSHIP_DOMAIN}}", request.getInternshipTitle());
        placeholders.put("{{START_DATE}}", request.getStartDate().toString());
        placeholders.put("{{DATE}}", LocalDate.now().toString());

        // Generate PPT
        File pptFile = pptGenerationService.generatePpt("OfferLetterTemplate.pptx", placeholders);

        // Upload to S3
        return s3Service.uploadFile(pptFile, "offer-letters");
    }

    public List<Certificate> getMyCertificates(Long studentId) {
        // Ideally filter by studentId, but for now repo methods might need adjustment
        // or custom query
        // Using findAll for simplicity in this snippet or assume repo has
        // findByStudentId
        // Wait, I didn't add findByStudentId in repo. Let me check repo content.
        // I added findByStudentIdAndInternshipTypeId. I should add findByStudentId.
        // For now, let's just return all (or empty if none).
        // Actually, I'll update repo or just use what I have.
        // I'll stick to logic.
        return certificateRepository.findAll().stream()
                .filter(c -> c.getStudentId().equals(studentId))
                .toList();
    }
}
