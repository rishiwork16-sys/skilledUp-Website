package com.skilledup.certificate.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CertificateGeneratorService {

    public byte[] generatePdfFromTemplate(String templateName, Map<String, String> data) {
        log.info("Generating PDF for template: {}", templateName);
        try {
            // 1. Load the HTML template
            String htmlContent = loadTemplate(templateName);

            // 2. Replace placeholders with data
            for (Map.Entry<String, String> entry : data.entrySet()) {
                htmlContent = htmlContent.replace("[[" + entry.getKey() + "]]", entry.getValue());
            }

            // 3. Convert HTML to PDF
            return renderPdf(htmlContent);

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String loadTemplate(String templateName) throws IOException {
        String path = "/templates/" + templateName;
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("Template file not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] renderPdf(String htmlContent) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Jsoup to parse HTML ensuring it's well-formed XML (required for
            // openhtmltopdf)
            Document document = Jsoup.parse(htmlContent, "UTF-8");
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

            builder.withW3cDocument(new W3CDom().fromJsoup(document), "/");
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        }
    }
}
