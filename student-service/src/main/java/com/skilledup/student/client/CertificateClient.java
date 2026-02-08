package com.skilledup.student.client;

import com.skilledup.student.dto.LORGenerationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "certificate-service", url = "${application.config.certificate-service-url:}")
// using url placeholder for local testing if needed, or rely on eureka
// "certificate-service"
public interface CertificateClient {

    @PostMapping("/api/certificates/lor/generate")
    String generateLOR(@RequestBody LORGenerationRequest request);

    @PostMapping("/api/certificates/internship/generate")
    String generateInternshipCertificate(@RequestBody LORGenerationRequest request);
}
