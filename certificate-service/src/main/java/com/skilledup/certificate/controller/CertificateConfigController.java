package com.skilledup.certificate.controller;

import com.skilledup.certificate.model.CertificateConfiguration;
import com.skilledup.certificate.service.CertificateConfigurationService;
import com.skilledup.certificate.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/certificates/config")
@RequiredArgsConstructor
public class CertificateConfigController {
    private final CertificateConfigurationService configurationService;
    private final S3Service s3Service;

    @GetMapping("/internship-template")
    public ResponseEntity<Map<String, String>> getInternshipCertificateTemplate() {
        CertificateConfiguration config = configurationService.getConfiguration();
        String key = config.getInternshipCertificateTemplateKey();
        String url = (key == null || key.isBlank()) ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }

    @PostMapping("/internship-template")
    public ResponseEntity<Map<String, String>> uploadInternshipCertificateTemplate(@RequestParam("file") MultipartFile file) {
        CertificateConfiguration updated = configurationService.uploadInternshipCertificateTemplate(file);
        String key = updated.getInternshipCertificateTemplateKey();
        String url = (key == null || key.isBlank()) ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }

    @GetMapping("/lor-template")
    public ResponseEntity<Map<String, String>> getLorTemplate() {
        CertificateConfiguration config = configurationService.getConfiguration();
        String key = config.getLorTemplateKey();
        String url = (key == null || key.isBlank()) ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }

    @PostMapping("/lor-template")
    public ResponseEntity<Map<String, String>> uploadLorTemplate(@RequestParam("file") MultipartFile file) {
        CertificateConfiguration updated = configurationService.uploadLorTemplate(file);
        String key = updated.getLorTemplateKey();
        String url = (key == null || key.isBlank()) ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }
}
