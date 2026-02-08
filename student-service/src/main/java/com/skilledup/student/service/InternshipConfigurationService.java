package com.skilledup.student.service;

import com.skilledup.student.dto.InternshipConfigurationDTO;
import com.skilledup.student.model.InternshipConfiguration;
import com.skilledup.student.repository.InternshipConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternshipConfigurationService {

    private final InternshipConfigurationRepository repository;
    private final S3Service s3Service;

    public InternshipConfiguration getConfiguration() {
        List<InternshipConfiguration> configs = repository.findAll();
        if (configs.isEmpty()) {
            // Create default
            InternshipConfiguration defaultConfig = InternshipConfiguration.builder().build();
            return repository.save(defaultConfig);
        }
        return configs.get(0);
    }

    public InternshipConfiguration updateConfiguration(InternshipConfigurationDTO dto) {
        InternshipConfiguration config = getConfiguration();

        if (dto.getAutoStartBatches() != null)
            config.setAutoStartBatches(dto.getAutoStartBatches());
        if (dto.getStartDay() != null)
            config.setStartDay(dto.getStartDay());
        if (dto.getFrequency() != null)
            config.setFrequency(dto.getFrequency());
        if (dto.getOfferLetterSubject() != null)
            config.setOfferLetterSubject(dto.getOfferLetterSubject());
        if (dto.getOfferLetterBody() != null)
            config.setOfferLetterBody(dto.getOfferLetterBody());
        if (dto.getOfferLetterTemplateKey() != null)
            config.setOfferLetterTemplateKey(dto.getOfferLetterTemplateKey());

        return repository.save(config);
    }

    public InternshipConfiguration uploadOfferLetterTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!original.endsWith(".pptx")) {
            throw new RuntimeException("Only .pptx template is supported");
        }

        InternshipConfiguration config = getConfiguration();
        File temp = null;
        try {
            temp = File.createTempFile("offer_letter_template_", ".pptx");
            file.transferTo(temp);

            String key = "internship/templates/offer-letter/" + System.currentTimeMillis() + "_OfferLetterTemplate.pptx";
            String storedKey = s3Service.uploadFile(key, temp);

            config.setOfferLetterTemplateKey(storedKey);
            config.setOfferLetterTemplateUpdatedAt(Instant.now());
            return repository.save(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload offer letter template", e);
        } finally {
            if (temp != null && temp.exists()) {
                temp.delete();
            }
        }
    }
}
