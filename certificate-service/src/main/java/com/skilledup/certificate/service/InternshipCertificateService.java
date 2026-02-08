package com.skilledup.certificate.service;

import com.skilledup.certificate.dto.LORGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipCertificateService {

    @Value("${file.output.dir:./generated-certificates}")
    private String outputDir;

    @Value("${file.templates.certificate:CertificateTemplate.pptx}")
    private String templateName;

    private final PptGenerationService pptGenerationService;
    private final S3Service s3Service;

    public String generateCertificate(LORGenerationRequest request) {
        log.info("Generating Certificate for student: {}", request.getStudentName());

        try {
            // Create output directory if not exists
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // Prepare Placeholders
            Map<String, String> placeholders = preparePlaceholders(request);

            log.info("Certificate Placeholders: {}", placeholders);

            // Generate PPT from Template
            File pptFile = pptGenerationService.generatePpt(templateName, placeholders);

            // Convert PPT to PDF
            String pdfFileName = "Certificate-"
                    + (request.getLorId() != null ? request.getLorId() : System.currentTimeMillis())
                    + ".pdf";
            File pdfFile = new File(outputDirectory, pdfFileName);

            convertPptToPdf(pptFile, pdfFile);

            // S3 Upload with Fallback
            String finalUrl;
            try {
                log.info("Attempting to upload Certificate to S3...");
                finalUrl = s3Service.uploadFile(pdfFile, "certificates");
                log.info("Certificate uploaded to S3: {}", finalUrl);
            } catch (Exception e) {
                log.error("S3 Upload Failed: {}", e.getMessage());
                // Fallback to Local URL
                finalUrl = "http://localhost:8083/api/certificates/download/" + pdfFileName;
                log.info("Falling back to Local URL: {}", finalUrl);
            }

            // Clean up PPT
            if (pptFile.exists()) {
                pptFile.delete();
            }

            return finalUrl;

        } catch (Exception e) {
            log.error("Failed to generate Certificate for student {}: {}", request.getStudentName(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Certificate PDF", e);
        }
    }

    private Map<String, String> preparePlaceholders(LORGenerationRequest request) {
        Map<String, String> placeholders = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy"); // e.g., 17 January 2026

        String studentName = request.getStudentName() != null ? request.getStudentName() : "Student";
        String domain = request.getDomain() != null ? request.getDomain() : "Internship";
        String issueDate = LocalDate.now().format(dateFormatter);

        // Standard Placeholders
        placeholders.put("{{STUDENT_NAME}}", studentName);
        placeholders.put("{{DOMAIN}}", domain);
        placeholders.put("{{START_DATE}}",
                request.getStartDate() != null ? request.getStartDate().format(dateFormatter) : "N/A");
        placeholders.put("{{END_DATE}}",
                request.getEndDate() != null ? request.getEndDate().format(dateFormatter) : "N/A");
        placeholders.put("{{DURATION}}", request.getDuration() != null ? request.getDuration() + " weeks" : "N/A");
        placeholders.put("{{CERTIFICATE_ID}}", request.getLorId() != null ? request.getLorId() : "N/A");
        placeholders.put("{{ISSUE_DATE}}", issueDate);

        // Workaround for Static Template Text
        // Workaround for Static Template Text - Adding variations for robustness
        placeholders.put("Vivek Singh", studentName);
        placeholders.put("Vivek  Singh", studentName); // Double space
        placeholders.put("Vivek Singh ", studentName); // Trailing space

        placeholders.put("MySQL & GenAI MasterClass with BigQuery", domain);
        placeholders.put("MySQL  &  GenAI  MasterClass  with  BigQuery", domain); // Wide spacing
        placeholders.put("MySQL & GenAI MasterClass with BigQuery.", domain + "."); // With dot

        placeholders.put("04 January 2026", issueDate);
        placeholders.put("04 January 2026.", issueDate + "."); // With dot

        return placeholders;
    }

    // Reuse conversion logic from LORService - simplified copy here to avoid
    // dependency cycle or public exposure valid
    // Ideally this should be in PptGenerationService but for now duplicating the
    // specific PDF conversion logic
    // actually PptGenerationService only does PPT generation. PDF conversion is
    // here.
    private void convertPptToPdf(File pptFile, File pdfFile) throws IOException {
        log.info("Converting Certificate PPTX to PDF: {}", pptFile.getName());

        try (FileInputStream fis = new FileInputStream(pptFile);
                XMLSlideShow ppt = new XMLSlideShow(fis);
                PDDocument pdfDocument = new PDDocument()) {

            Dimension pgsize = ppt.getPageSize();
            List<XSLFSlide> slides = ppt.getSlides();
            float scale = 2.0f; // High quality
            int width = (int) (pgsize.getWidth() * scale);
            int height = (int) (pgsize.getHeight() * scale);

            for (XSLFSlide slide : slides) {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = img.createGraphics();

                // Enhanced rendering hints for better font quality
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                graphics.scale(scale, scale);
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
                slide.draw(graphics);
                graphics.dispose();

                PDPage pdfPage = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                pdfDocument.addPage(pdfPage);

                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, pdfPage)) {
                    PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
                            .createFromImage(pdfDocument, img);
                    contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                }
            }
            pdfDocument.save(pdfFile);
        } catch (Exception e) {
            throw new IOException("Failed to convert Certificate PPT to PDF", e);
        }
    }
}
