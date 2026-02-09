package com.skilledup.certificate.controller;

import com.skilledup.certificate.dto.ApiMessage;
import com.skilledup.certificate.dto.LORGenerationRequest;
import com.skilledup.certificate.service.CertificateGeneratorService;
import com.skilledup.certificate.service.LORGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateGeneratorService certificateGeneratorService;
    private final LORGenerationService lorGenerationService;

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Certificate Service is running"));
    }

    // Test Endpoint to Preview Certificate
    @PostMapping("/preview")
    public ResponseEntity<byte[]> previewCertificate(@RequestBody Map<String, String> requestData) {

        // Default Mock Data if missing
        Map<String, String> data = new HashMap<>(requestData);
        data.putIfAbsent("studentName", "John Doe");
        data.putIfAbsent("domain", "Full Stack Development");
        data.putIfAbsent("startDate", "01 Jan 2025");
        data.putIfAbsent("endDate", "28 Feb 2025");
        data.putIfAbsent("issueDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        data.putIfAbsent("credentialId", "SK-" + System.currentTimeMillis());

        byte[] pdfBytes = certificateGeneratorService.generatePdfFromTemplate("certificate_template.html", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Test Endpoint to Preview Exit Letter
    @PostMapping("/exit-letter/preview")
    public ResponseEntity<byte[]> previewExitLetter(@RequestBody Map<String, String> requestData) {

        // Default Mock Data if missing
        Map<String, String> data = new HashMap<>(requestData);
        data.putIfAbsent("studentName", "John Doe");
        data.putIfAbsent("domain", "Full Stack Development");
        data.putIfAbsent("startDate", "01 Jan 2025");
        data.putIfAbsent("endDate", "28 Feb 2025");
        data.putIfAbsent("issueDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        byte[] pdfBytes = certificateGeneratorService.generatePdfFromTemplate("exit_letter_template.html", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=exit_letter.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Test Endpoint to Preview Recommendation Letter
    @PostMapping("/recommendation/preview")
    public ResponseEntity<byte[]> previewRecommendationLetter(@RequestBody Map<String, String> requestData) {

        try {
            LORGenerationRequest request = new LORGenerationRequest();
            request.setStudentName(requestData.getOrDefault("studentName", "John Doe"));
            request.setDomain(requestData.getOrDefault("domain", "Software Engineering"));
            request.setStartDate(LocalDate.now().minusMonths(6)); // Mock
            request.setEndDate(LocalDate.now()); // Mock
            request.setDuration(24);
            request.setCompletionPercent(Double.parseDouble(requestData.getOrDefault("score", "98")));
            request.setLorId("LOR-TEST-" + System.currentTimeMillis());

            String pdfPath = lorGenerationService.generateLOR(request);
            File pdfFile = new File(pdfPath);
            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=recommendation_letter.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint for Service-to-Service LOR Generation
    @PostMapping("/lor/generate")
    public ResponseEntity<String> generateLOR(@RequestBody LORGenerationRequest request) {
        try {
            String lorUrl = lorGenerationService.generateLOR(request);
            return ResponseEntity.ok(lorUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate LOR: " + e.getMessage());
        }
    }

    private final com.skilledup.certificate.service.InternshipCertificateService internshipCertificateService;

    // Endpoint for Service-to-Service Internship Certificate Generation
    @PostMapping("/internship/generate")
    public ResponseEntity<String> generateInternshipCertificate(@RequestBody LORGenerationRequest request) {
        try {
            String certificateUrl = internshipCertificateService.generateCertificate(request);
            return ResponseEntity.ok(certificateUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate Certificate: " + e.getMessage());
        }
    }

    // New Endpoint to Download LOR
    @GetMapping("/lor/download/{fileName}")
    public ResponseEntity<Resource> downloadLOR(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("generated-lors").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint to Download Certificate (Local Fallback)
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("generated-certificates").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
