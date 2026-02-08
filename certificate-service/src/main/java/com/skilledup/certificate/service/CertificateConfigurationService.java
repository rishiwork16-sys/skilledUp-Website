package com.skilledup.certificate.service;

import com.skilledup.certificate.model.CertificateConfiguration;
import com.skilledup.certificate.repository.CertificateConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateConfigurationService {
    private final CertificateConfigurationRepository repository;
    private final S3Service s3Service;

    public CertificateConfiguration getConfiguration() {
        List<CertificateConfiguration> configs = repository.findAll();
        if (configs.isEmpty()) {
            return repository.save(CertificateConfiguration.builder().build());
        }
        return configs.get(0);
    }

    public CertificateConfiguration uploadInternshipCertificateTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!original.endsWith(".pptx")) {
            throw new RuntimeException("Only .pptx template is supported");
        }

        CertificateConfiguration config = getConfiguration();
        File temp = null;
        try {
            temp = File.createTempFile("internship_certificate_template_", ".pptx");
            file.transferTo(temp);

            String key = "internship/templates/certificate/" + System.currentTimeMillis() + "_CertificateTemplate.pptx";
            String storedKey = s3Service.uploadTemplate(key, temp);
            config.setInternshipCertificateTemplateKey(storedKey);
            config.setInternshipCertificateTemplateUpdatedAt(Instant.now());
            return repository.save(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload internship certificate template", e);
        } finally {
            if (temp != null && temp.exists()) {
                temp.delete();
            }
        }
    }

    public CertificateConfiguration uploadLorTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!original.endsWith(".pptx")) {
            throw new RuntimeException("Only .pptx template is supported");
        }

        CertificateConfiguration config = getConfiguration();
        File temp = null;
        try {
            temp = File.createTempFile("lor_template_", ".pptx");
            file.transferTo(temp);

            String key = "internship/templates/lor/" + System.currentTimeMillis() + "_LetterOfRecommendation.pptx";
            String storedKey = s3Service.uploadTemplate(key, temp);
            config.setLorTemplateKey(storedKey);
            config.setLorTemplateUpdatedAt(Instant.now());
            return repository.save(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload LOR template", e);
        } finally {
            if (temp != null && temp.exists()) {
                temp.delete();
            }
        }
    }
}
