package com.skilledup.student.controller;

import com.skilledup.student.dto.InternshipConfigurationDTO;
import com.skilledup.student.model.InternshipConfiguration;
import com.skilledup.student.service.InternshipConfigurationService;
import com.skilledup.student.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/student/config")
@RequiredArgsConstructor
public class InternshipConfigurationController {

    private final InternshipConfigurationService service;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<InternshipConfiguration> getConfig() {
        return ResponseEntity.ok(service.getConfiguration());
    }

    @PutMapping
    public ResponseEntity<InternshipConfiguration> updateConfig(@RequestBody InternshipConfigurationDTO dto) {
        return ResponseEntity.ok(service.updateConfiguration(dto));
    }

    @GetMapping("/offer-letter-template")
    public ResponseEntity<Map<String, String>> getOfferLetterTemplate() {
        InternshipConfiguration config = service.getConfiguration();
        String key = config.getOfferLetterTemplateKey();
        String url = (key == null || key.isBlank()) ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }

    @PostMapping("/offer-letter-template")
    public ResponseEntity<Map<String, String>> uploadOfferLetterTemplate(@RequestParam("file") MultipartFile file) {
        InternshipConfiguration updated = service.uploadOfferLetterTemplate(file);
        String key = updated.getOfferLetterTemplateKey();
        String url = key == null || key.isBlank() ? "" : s3Service.generatePresignedUrl(key);
        return ResponseEntity.ok(Map.of(
                "key", key == null ? "" : key,
                "url", url
        ));
    }
}
