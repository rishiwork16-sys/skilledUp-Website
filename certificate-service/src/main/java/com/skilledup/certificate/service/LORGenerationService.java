package com.skilledup.certificate.service;

import com.skilledup.certificate.dto.LORGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
// import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory; // Deprecated/Older?
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LORGenerationService {

    @Value("${file.output.dir:./generated-lors}")
    private String outputDir;

    private final PptGenerationService pptGenerationService;
    private final S3Service s3Service;

    public String generateLOR(LORGenerationRequest request) {
        // Use the new template provided by user
        String templateName = "Letter of Recommendation.pptx";
        log.info("Generating LOR for student: {}", request.getStudentName());

        try {
            // Create output directory if not exists
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // Prepare Placeholders
            Map<String, String> placeholders = preparePlaceholders(request);

            // Dynamic Current Date for "Date:" field
            // Format: "17th January, 2026"
            LocalDate today = LocalDate.now();
            String daySuffix = getDaySuffix(today.getDayOfMonth());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d'" + daySuffix + "' MMMM, yyyy");
            String formattedDate = today.format(formatter);

            placeholders.put("1st December, 2025", formattedDate); // Replace specific template date
            placeholders.put("{{CURRENT_DATE}}", formattedDate);

            // Log the mappings for debugging
            log.info("LOR Placeholders: {}", placeholders);

            // Generate PPT from Template (The user requested PPT format)
            File pptFile = pptGenerationService.generatePpt("Letter of Recommendation.pptx", placeholders);

            // Convert PPT to PDF with Enhanced Rendering
            String pdfFileName = "LOR-" + (request.getLorId() != null ? request.getLorId() : System.currentTimeMillis())
                    + ".pdf";
            File pdfFile = new File(outputDirectory, pdfFileName);

            convertPptToPdf(pptFile, pdfFile);

            // S3 Upload with Fallback
            String finalUrl;
            try {
                log.info("Attempting to upload LOR to S3...");
                finalUrl = s3Service.uploadFile(pdfFile, "lor-documents");
                log.info("LOR uploaded to S3: {}", finalUrl);
            } catch (Exception e) {
                log.error("S3 Upload Failed: {}", e.getMessage());
                // Fallback to Local URL
                finalUrl = "http://localhost:8083/api/certificates/lor/download/" + pdfFileName;
                log.info("Falling back to Local URL: {}", finalUrl);
            }

            // Clean up PPT (Keep PDF for local serving)
            if (pptFile.exists()) {
                pptFile.delete();
            }

            return finalUrl;

        } catch (Exception e) {
            log.error("Failed to generate LOR for student {}: {}", request.getStudentName(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate LOR PDF", e);
        }
    }

    private String getDaySuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
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

    private Map<String, String> preparePlaceholders(LORGenerationRequest request) {
        Map<String, String> placeholders = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        placeholders.put("{{STUDENT_NAME}}", request.getStudentName() != null ? request.getStudentName() : "Student");
        placeholders.put("Rahul Singh", request.getStudentName() != null ? request.getStudentName() : "Rahul Singh"); // Fallback
        placeholders.put("Rahul", request.getStudentName() != null ? request.getStudentName() : "Rahul"); // Body text
                                                                                                          // replacement
        placeholders.put("{{DOMAIN}}", request.getDomain() != null ? request.getDomain() : "Internship");
        placeholders.put("{{START_DATE}}",
                request.getStartDate() != null ? request.getStartDate().format(dateFormatter) : "N/A");
        placeholders.put("1st October 2025",
                request.getStartDate() != null ? request.getStartDate().format(dateFormatter) : "Oct 01, 2025"); // Fallback

        placeholders.put("{{END_DATE}}",
                request.getEndDate() != null ? request.getEndDate().format(dateFormatter) : "N/A");
        placeholders.put("30th November 2025",
                request.getEndDate() != null ? request.getEndDate().format(dateFormatter) : "Nov 30, 2025"); // Fallback

        placeholders.put("Data Science", request.getDomain() != null ? request.getDomain() : "Internship"); // Fallback
        placeholders.put("Data Science Internship",
                request.getDomain() != null ? request.getDomain() + " Internship" : "Internship"); // Fallback for full
                                                                                                   // string

        placeholders.put("{{DURATION}}", request.getDuration() != null ? request.getDuration() + " weeks" : "N/A");
        placeholders.put("{{EMAIL}}", request.getEmail() != null ? request.getEmail() : "N/A");
        placeholders.put("test@gmail.com", request.getEmail() != null ? request.getEmail() : "test@gmail.com"); // Fallback
                                                                                                                // for
                                                                                                                // hardcoded

        // Map LOR_ID and INTERN_ID
        // request.getLorId() now holds the INTYYMM... format from Student Service
        String internId = request.getLorId() != null ? request.getLorId() : "N/A";
        placeholders.put("{{INTERN_ID}}", internId);
        placeholders.put("INTYYMM00001", internId); // Fallback for hardcoded placeholder
        placeholders.put("{{LOR_ID}}", internId);

        placeholders.put("{{ISSUE_DATE}}", LocalDate.now().format(dateFormatter));

        return placeholders;
    }

    private void convertPptToPdf(File pptFile, File pdfFile) throws IOException {
        log.info("Converting PPTX to PDF with Enhanced Rendering: {}", pptFile.getName());

        try (FileInputStream fis = new FileInputStream(pptFile);
                XMLSlideShow ppt = new XMLSlideShow(fis);
                PDDocument pdfDocument = new PDDocument()) {

            Dimension pgsize = ppt.getPageSize();
            List<XSLFSlide> slides = ppt.getSlides();

            // Scale factor for better quality (2x)
            float scale = 2.0f;
            int width = (int) (pgsize.getWidth() * scale);
            int height = (int) (pgsize.getHeight() * scale);

            for (XSLFSlide slide : slides) {
                // Use ARGB for transparency support
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = img.createGraphics();

                // Advanced Rendering Hints for Text Quality
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                // Scale graphics
                graphics.scale(scale, scale);

                // Fill background with White (PPT default) - Safe ensuring no transparency
                // issues
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

                // Draw slide
                slide.draw(graphics);
                graphics.dispose();

                // Create PDF page (Match image size or scale back?)
                // PDF page size should match PPT size points.
                PDPage pdfPage = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                pdfDocument.addPage(pdfPage);

                // Draw image to PDF page
                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, pdfPage)) {
                    PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
                            .createFromImage(pdfDocument, img);
                    contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                }
            }

            pdfDocument.save(pdfFile);
            log.info("PDF conversion completed: {}", pdfFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error during PPT to PDF conversion", e);
            throw new IOException("Failed to convert PPT to PDF", e);
        }
    }
}
