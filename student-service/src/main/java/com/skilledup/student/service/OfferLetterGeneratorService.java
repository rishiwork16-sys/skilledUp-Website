package com.skilledup.student.service;

import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.Student;
import com.skilledup.student.model.InternshipConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class OfferLetterGeneratorService {

    private final InternshipConfigurationService configurationService;
    private final S3Service s3Service;

    @org.springframework.beans.factory.annotation.Value("${file.templates.dir}")
    private String templatesDir;

    @org.springframework.beans.factory.annotation.Value("${file.templates.offer-letter}")
    private String offerLetterTemplateName;

    public OfferLetterGeneratorService(InternshipConfigurationService configurationService, S3Service s3Service) {
        this.configurationService = configurationService;
        this.s3Service = s3Service;
    }

    public File generateOfferLetter(InternshipEnrollment enrollment) {
        String templatePath = templatesDir + (templatesDir.endsWith("/") ? "" : "/") + offerLetterTemplateName;
        try (InputStream templateStream = getTemplateStream(templatePath);
                XMLSlideShow ppt = new XMLSlideShow(templateStream)) {

            Student student = enrollment.getStudent();

            // Map placeholders to values
            Map<String, String> replacements = new java.util.HashMap<>();
            // Comprehensive Replacement Map
            replacements.put("{Student Name}", student.getName());
            replacements.put("Rahul Singh", student.getName());
            replacements.put("Student Name", student.getName()); // Just in case

            replacements.put("{Internship Domain}", enrollment.getInternshipCategory().getTitle());
            replacements.put("Data Science", enrollment.getInternshipCategory().getTitle());
            replacements.put("Data Science Intern", enrollment.getInternshipCategory().getTitle() + " Intern");

            String formattedDate = formatDate();
            replacements.put("{Date}", formattedDate);
            replacements.put("{Start Date}", formattedDate);
            replacements.put("29th September 2025", formattedDate); // Template specific date

            replacements.put("{Duration}", enrollment.getDuration() + " Months");

            replacements.put("rishiwork16@gmail.com", student.getEmail());
            replacements.put("test@gmail.com", student.getEmail());
            replacements.put("{Email}", student.getEmail());

            String internId = generateInternId(enrollment);
            replacements.put("INTYYMM00001", internId);
            replacements.put("INT260100001", internId); // Template specific ID
            replacements.put("{InternId}", internId);

            // Iterate over all slides and shapes to replace text
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        boolean textReplaced = false; // Flag to track if replacement occurred

                        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                            // 1. Try "Safe" Replacement (Run-level) to preserve formatting
                            boolean safeReplaced = false;
                            for (XSLFTextRun run : paragraph.getTextRuns()) {
                                String text = run.getRawText();
                                log.info("Found Text Run: '{}'", text); // Debug log
                                if (text != null && !text.isEmpty()) {
                                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                        if (text.contains(entry.getKey())) {
                                            text = text.replace(entry.getKey(), entry.getValue());
                                            run.setText(text);
                                            safeReplaced = true;
                                            textReplaced = true;
                                        }
                                    }
                                }
                            }

                            // 2. If "Safe" replacement didn't happen, check for "Split" matches (Merge
                            // approach)
                            StringBuilder paragraphTextBuilder = new StringBuilder();
                            for (XSLFTextRun run : paragraph.getTextRuns()) {
                                paragraphTextBuilder.append(run.getRawText());
                            }
                            String paragraphText = paragraphTextBuilder.toString();

                            boolean needsMerge = false;
                            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                if (paragraphText.contains(entry.getKey())) {
                                    needsMerge = true;
                                }
                            }

                            if (needsMerge) {
                                boolean mergedReplaced = false;
                                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                    if (paragraphText.contains(entry.getKey())) {
                                        paragraphText = paragraphText.replace(entry.getKey(), entry.getValue());
                                        mergedReplaced = true;
                                        textReplaced = true;
                                    }
                                }

                                if (mergedReplaced) {
                                    // Nuclear option: Merge into first run
                                    java.util.List<XSLFTextRun> runs = paragraph.getTextRuns();
                                    if (!runs.isEmpty()) {
                                        XSLFTextRun firstRun = runs.get(0);
                                        firstRun.setText(paragraphText);
                                        for (int i = 1; i < runs.size(); i++) {
                                            runs.get(i).setText("");
                                        }
                                    }
                                }
                            }
                        }

                        if (textReplaced) {
                            String fullText = textShape.getText();
                            if (fullText != null && (fullText.contains("Email") || fullText.contains("@") || fullText.contains("Date") || fullText.contains("Intern Id") || fullText.contains("InternId") || fullText.contains("INT"))) {
                                java.awt.geom.Rectangle2D anchor = textShape.getAnchor();
                                double desiredWidth = 360;
                                if (anchor != null && anchor.getWidth() < desiredWidth) {
                                    textShape.setAnchor(new java.awt.geom.Rectangle2D.Double(
                                            anchor.getX(),
                                            anchor.getY(),
                                            desiredWidth,
                                            anchor.getHeight()
                                    ));
                                }
                            }
                        }
                    }
                }
            }

            /*
            XSLFTextShape bodyShape = null;
            XSLFTextShape footerHeaderShape = null; // "For Skilledup..."
            XSLFTextShape signatoryNameShape = null; // "Vijay Narayan Singh"
            XSLFTextShape signatoryTitleShape = null; // "CEO"
            XSLFPictureShape signaturePic = null;
            XSLFPictureShape stampPic = null;

            double slideHeight = ppt.getPageSize().getHeight();
            double slideWidth = ppt.getPageSize().getWidth();
            double footerThresholdY = slideHeight * 0.40; // Only consider elements in bottom 60% as footer

            // --- 1. Shape Identification Loop ---
            // 1. Identify Shapes
            XSLFTextShape dateShape = null;
            XSLFTextShape emailShape = null;
            XSLFTextShape internIdShape = null;
            */

            // Save to temp file
            File outputFile = File.createTempFile("offer_letter_" + student.getId() + "_", ".pptx");
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                ppt.write(fos);
            }
            return outputFile;

        } catch (IOException e) {
            log.error("Failed to generate PPTX offer letter", e);
            throw new RuntimeException("Offer letter generation failed", e);
        }
    }

    private InputStream getTemplateStream(String fallbackLocalPath) throws IOException {
        try {
            InternshipConfiguration config = configurationService.getConfiguration();
            String key = config.getOfferLetterTemplateKey();
            if (key != null && !key.isBlank()) {
                return s3Service.downloadFile(key);
            }
        } catch (Exception ignored) {
        }
        File localFile = new File(fallbackLocalPath);
        if (localFile.exists()) {
            return new FileInputStream(localFile);
        }
        InputStream cp = getClass().getResourceAsStream("/templates/" + offerLetterTemplateName);
        if (cp != null) {
            return cp;
        }
        throw new FileNotFoundException("Offer letter template not found");
    }

    private String formatDate() {
        LocalDate date = LocalDate.now();
        int day = date.getDayOfMonth();
        String suffix = getDayNumberSuffix(day);
        return date.format(DateTimeFormatter.ofPattern("d'" + suffix + "' MMMM yyyy"));
    }

    private String getDayNumberSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }

        switch (day % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private String generateInternId(InternshipEnrollment enrollment) {
        // Use enrollment date or current date if null
        LocalDate date = LocalDate.now();
        if (enrollment.getEnrolledAt() != null) {
            date = enrollment.getEnrolledAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String yyMm = date.format(DateTimeFormatter.ofPattern("yyMM"));
        return String.format("INT%s%05d", yyMm, enrollment.getId());
    }
}
