package com.skilledup.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.awt.Color;

@Service
@RequiredArgsConstructor
@Slf4j
public class PptGenerationService {

    @Value("${file.templates.dir}")
    private String templateDir;

    @Value("${file.templates.certificate:CertificateTemplate.pptx}")
    private String certificateTemplateName;

    @Value("${file.templates.lor:Letter of Recommendation.pptx}")
    private String lorTemplateName;

    private final CertificateConfigurationService configurationService;
    private final S3Service s3Service;

    public File generatePpt(String templateName, Map<String, String> placeholders) {
        log.info("Starting PPT Generation with Placeholders: {}", placeholders.keySet()); // DEBUG LOG
        // Try to find the file in configured directory first, then classpath
        InputStream templateStream = null;
        try {
            templateStream = resolveTemplateStream(templateName);

            String outputFileName = "generated_" + UUID.randomUUID() + ".pptx";
            File outputFile = new File(System.getProperty("java.io.tmpdir"), outputFileName);

            try (InputStream fis = templateStream;
                    XMLSlideShow ppt = new XMLSlideShow(fis);
                    FileOutputStream fos = new FileOutputStream(outputFile)) {

                // Iterate over all slides
                for (XSLFSlide slide : ppt.getSlides()) {
                    // Iterate over all shapes in the slide
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            XSLFTextShape txShape = (XSLFTextShape) shape;
                            String shapeText = txShape.getText();
                            log.info("Processing Shape Text: '{}'", shapeText); // DEBUG LOG

                            // Strategy 1: If "Vijay Narayan Singh" is in a SEPARATE shape from "For
                            // SkilledUp",
                            // move the entire shape DOWN to create a gap for the signature.
                            if (shapeText != null && shapeText.contains("Vijay Narayan Singh")
                                    && !shapeText.contains("For SkilledUp")) {
                                java.awt.geom.Rectangle2D anchor = txShape.getAnchor();
                                // Move down 40 points (Reduced from 50 to tighted gap "thoda sa aur")
                                txShape.setAnchor(new java.awt.geom.Rectangle2D.Double(
                                        anchor.getX(), anchor.getY() + 40, anchor.getWidth(), anchor.getHeight()));
                                log.info("Adjusted Layout: Moved 'Vijay Narayan Singh' Text Box DOWN by 40pts.");
                            }

                            // Check each paragraph
                            for (XSLFTextParagraph paragraph : txShape.getTextParagraphs()) {

                                // Strategy 2: If "For SkilledUp" and Signer Name are in the SAME shape,
                                // use SpaceAfter to push the next paragraph down.
                                String paraText = paragraph.getText();
                                if (paraText != null
                                        && (paraText.contains("For SkilledUp") || paraText.contains("for SkilledUp"))) {
                                    // Reset line spacing to normal (100%)
                                    paragraph.setLineSpacing(100.0);
                                    // Add Vertical Space AFTER this paragraph
                                    // 60.0 points (Reduced from 80 to tighten gap)
                                    paragraph.setSpaceAfter(60.0);
                                    log.info("Adjusted Layout: Added SpaceAfter=60pt to 'For SkilledUp'.");
                                }

                                // PARAGRAPH LEVEL RICH TEXT REPLACEMENT (Bold Dynamic Data)
                                String fullParaText = paragraph.getText();

                                // STEP 1: Run-Level Replacement (Preserves Template Styles + Applies Custom)
                                boolean runReplacementOccurred = false;
                                for (XSLFTextRun run : paragraph.getTextRuns()) {
                                    String text = run.getRawText();
                                    if (text != null && !text.isEmpty()) {
                                        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                                            if (text.contains(entry.getKey())) {
                                                log.info("Run-Level Replacement: Replacing '{}' with '{}'",
                                                        entry.getKey(), entry.getValue());
                                                text = text.replace(entry.getKey(), entry.getValue());
                                                run.setText(text);
                                                runReplacementOccurred = true;

                                                // --- APPLY CUSTOM STYLING ---
                                                String key = entry.getKey();
                                                String val = entry.getValue();

                                                boolean isStudentName = key.contains("STUDENT_NAME")
                                                        || val.equals("Vivek Singh");
                                                boolean isDomain = key.contains("DOMAIN") || val.contains("MySQL");
                                                boolean isDate = key.contains("ISSUE_DATE") || val.contains("2026");

                                                if (isStudentName) {
                                                    log.info("Applying Gold/Bold/Italic Styling to Student Name");
                                                    run.setFontFamily("Monotype Corsiva");
                                                    run.setFontColor(new java.awt.Color(212, 175, 55)); // Gold Color
                                                    run.setFontSize(50.0);
                                                    run.setBold(true);
                                                    run.setItalic(true);
                                                } else if (isDomain) {
                                                    log.info("Applying Blue/Bold Styling to Domain");
                                                    run.setFontFamily("Arial");
                                                    run.setFontColor(new java.awt.Color(30, 80, 180)); // Royal Blue
                                                    run.setBold(true);
                                                } else if (isDate) {
                                                    log.info("Applying Blue/Bold Styling to Date");
                                                    run.setFontFamily("Arial");
                                                    run.setFontColor(new java.awt.Color(30, 80, 180)); // Royal Blue
                                                    run.setBold(true);
                                                }
                                            }
                                        }
                                    }
                                }

                                // STEP 2: Aggressive Reconstruction (Fallback for Split Placeholders)
                                // Only trigger if keys still exist in the paragraph text
                                fullParaText = paragraph.getText();
                                boolean needsReconstruction = false;
                                if (fullParaText != null && !fullParaText.isEmpty()) {
                                    for (String key : placeholders.keySet()) {
                                        if (fullParaText.contains(key)) {
                                            needsReconstruction = true;
                                            break;
                                        }
                                    }
                                }

                                if (needsReconstruction) {
                                    log.info("Rich Text Reconstruction triggered for: {}", fullParaText);

                                    // 2. Perform Replacements on Full Text first to get the final string
                                    // We need to track *what* was replaced to know what to BOLD.
                                    // This is tricky. Simplified approach:
                                    // Replace everything. Then manually find the inserted values and separate them
                                    // into runs?
                                    // No, that risks finding "Rahul" in standard text.

                                    // Robust Parsing Strategy:
                                    // We will tokenize the string by our Keys.
                                    // "This is {{NAME}} from..." -> ["This is ", "{{NAME}}", " from..."]
                                    // But we have multiple keys.

                                    // Let's use a simpler heuristic for this specific request:
                                    // 1. Perform standard replacement to get Final String.
                                    // 2. Identify the *values* that were inserted.
                                    // 3. Rebuild runs: Scan final string. If match found for an inserted value ->
                                    // Make Bold Run. Else -> Normal Run.

                                    String finalText = fullParaText;
                                    java.util.List<String> valuesToBold = new java.util.ArrayList<>();

                                    // Order matters! sort keys by length descending to avoid partial replacements
                                    // (e.g. "Name" vs "Surname") mechanism
                                    // But here keys are distinct enough.
                                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                                        if (finalText.contains(entry.getKey())) {
                                            String val = entry.getValue();
                                            finalText = finalText.replace(entry.getKey(), val);
                                            // Only bold interesting fields (Name, ID, Date, Domain)
                                            // Not "Internship" or generic words if possible, but user wants "Dynamic
                                            // Data" bold.
                                            // Let's bold ALL dynamic replacements to be safe/consistent with request.
                                            valuesToBold.add(val);
                                        }
                                    }

                                    // 3. Clear existing runs
                                    List<XSLFTextRun> runs = paragraph.getTextRuns();
                                    // We need to remove all runs. removing from list might not work directly in POI
                                    // Safest: Set text of first run to "", remove others?
                                    // Iterate backwards to remove
                                    for (int i = runs.size() - 1; i >= 0; i--) {
                                        paragraph.removeTextRun(runs.get(i));
                                    }

                                    // 4. Reconstruct Runs (Tokenizing by values)
                                    // Naive approach: Find first occurrence of ANY value.
                                    // "Hello Rahul Singh, welcome." Value="Rahul Singh"
                                    // We need to tokenize sequentially.

                                    // Work on a temp string buffer?
                                    // Let's use a pointer index.
                                    String remaining = finalText;
                                    while (!remaining.isEmpty()) {
                                        // Find the earliest occurring value
                                        int earliestIndex = -1;
                                        String foundValue = null;

                                        for (String val : valuesToBold) {
                                            int idx = remaining.indexOf(val);
                                            if (idx != -1) {
                                                if (earliestIndex == -1 || idx < earliestIndex) {
                                                    earliestIndex = idx;
                                                    foundValue = val;
                                                } else if (idx == earliestIndex) {
                                                    // If same start index, pick LONGER one to be greedy
                                                    // e.g. "Data Science" vs "Data"
                                                    if (val.length() > foundValue.length()) {
                                                        foundValue = val;
                                                    }
                                                }
                                            }
                                        }

                                        if (earliestIndex == -1) {
                                            // No more values found, append rest as Normal
                                            XSLFTextRun run = paragraph.addNewTextRun();
                                            run.setText(remaining);
                                            run.setFontFamily("Arial");
                                            run.setFontColor(Color.BLACK);
                                            run.setBold(false); // Normal
                                            break;
                                        }

                                        // We found a value!
                                        // 1. Append text BEFORE value (Normal)
                                        if (earliestIndex > 0) {
                                            String preText = remaining.substring(0, earliestIndex);
                                            XSLFTextRun run = paragraph.addNewTextRun();
                                            run.setText(preText);
                                            run.setFontFamily("Arial");
                                            run.setFontColor(Color.BLACK);
                                            run.setBold(false);
                                        }

                                        // 2. Append VALUE (Bold or Styled)
                                        XSLFTextRun boldRun = paragraph.addNewTextRun();
                                        boldRun.setText(foundValue);

                                        // --- CUSTOM STYLING LOGIC ---

                                        // 1. Identify what this value represents
                                        boolean isStudentName = foundValue.equals(placeholders.get("{{STUDENT_NAME}}"))
                                                ||
                                                foundValue.equals(placeholders.get("Vivek Singh"));

                                        boolean isDomain = foundValue.equals(placeholders.get("{{DOMAIN}}")) ||
                                                foundValue.equals(
                                                        placeholders.get("MySQL & GenAI MasterClass with BigQuery"));

                                        boolean isDate = foundValue.equals(placeholders.get("{{ISSUE_DATE}}")) ||
                                                foundValue.equals(placeholders.get("04 January 2026"));

                                        if (isStudentName) {
                                            // Call new helper method for robust font handling
                                            applyStudentNameStyle(boldRun);
                                        } else if (isDomain) {
                                            // Blue Color for Domain
                                            boldRun.setFontColor(new Color(30, 80, 180)); // Royal Blue
                                            boldRun.setFontFamily("Arial");
                                            boldRun.setBold(true);
                                        } else if (isDate) {
                                            // Blue Color for Date
                                            boldRun.setFontColor(new Color(30, 80, 180)); // Royal Blue
                                            boldRun.setBold(true);
                                            boldRun.setFontFamily("Arial");
                                        } else if (foundValue.contains("@")) {
                                            // Email Style
                                            boldRun.setFontColor(Color.BLUE);
                                            boldRun.setUnderlined(true);
                                        } else {
                                            // Default Dynamic Data Style
                                            boldRun.setFontColor(Color.BLACK);
                                            boldRun.setBold(true);
                                        }

                                        // 3. Advance
                                        remaining = remaining.substring(earliestIndex + foundValue.length());
                                    }
                                }
                                // Loop removed (was forcing Arial/Black and destroying styles)
                            }
                        } else if (shape instanceof XSLFPictureShape) {
                            // Check for Signature/Stamp Images (usually at bottom)
                            XSLFPictureShape picShape = (XSLFPictureShape) shape;
                            java.awt.geom.Rectangle2D anchor = picShape.getAnchor();

                            // Assume signature is in bottom half (Y > 300)
                            if (anchor.getY() > 300) {
                                // Move Picture DOWN by 35 points (Safe High Position)
                                picShape.setAnchor(new java.awt.geom.Rectangle2D.Double(anchor.getX(),
                                        anchor.getY() + 35, anchor.getWidth(), anchor.getHeight()));
                                log.info("Adjusted Layout: Moved Picture/Signature DOWN by 35pts.");
                            }
                        }
                    }
                }

                ppt.write(fos);
                return outputFile;

            }
        } catch (

        IOException e) {
            log.error("Error generating PPT from template {}: {}", templateName, e.getMessage());
            throw new RuntimeException("Failed to generate certificate PPT", e);
        } finally {
            // Stream is closed in try-with-resources
        }
    }

    private InputStream resolveTemplateStream(String templateName) throws IOException {
        if (templateName != null && templateName.equalsIgnoreCase(certificateTemplateName)) {
            try {
                String key = configurationService.getConfiguration().getInternshipCertificateTemplateKey();
                if (key != null && !key.isBlank()) {
                    log.info("Loading internship certificate template from S3: {}", key);
                    return s3Service.downloadFile(key);
                }
            } catch (Exception ignored) {
            }
        }
        if (templateName != null && templateName.equalsIgnoreCase(lorTemplateName)) {
            try {
                String key = configurationService.getConfiguration().getLorTemplateKey();
                if (key != null && !key.isBlank()) {
                    log.info("Loading LOR template from S3: {}", key);
                    return s3Service.downloadFile(key);
                }
            } catch (Exception ignored) {
            }
        }

        File localFile = new File(templateDir, templateName);
        if (localFile.exists()) {
            log.info("Loading template from file system: {}", localFile.getAbsolutePath());
            return new FileInputStream(localFile);
        }

        log.info("Template not found at {}, trying classpath resources...", localFile.getAbsolutePath());
        InputStream cp = getClass().getResourceAsStream("/templates/" + templateName);
        if (cp == null) {
            throw new FileNotFoundException("Template not found in classpath: /templates/" + templateName);
        }
        return cp;
    }

    private void applyStudentNameStyle(XSLFTextRun run) {
        // Log available fonts once to debug
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        boolean hasMonotype = false;
        for (String f : fonts) {
            if (f.equalsIgnoreCase("Monotype Corsiva")) {
                hasMonotype = true;
                break;
            }
        }

        run.setFontColor(new Color(218, 165, 32)); // GoldenRod
        if (hasMonotype) {
            run.setFontFamily("Monotype Corsiva");
            run.setItalic(true); // Monotype Corsiva is often treated as Italic
        } else {
            log.warn("Monotype Corsiva font not found! Creating fallback.");
            run.setFontFamily("Brush Script MT"); // Fallback cursive
            run.setItalic(true);
        }
        run.setFontSize(50.0);
        run.setBold(true);
    }
}
