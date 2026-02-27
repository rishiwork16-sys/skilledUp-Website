package com.skilledup.course.controller;

import com.skilledup.course.entity.Course;
import com.skilledup.course.repository.CourseRepository;
import com.skilledup.course.service.S3Service;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * CourseShareController — Social Media OG Tag Share Page
 *
 * Aise kaam karta hai:
 * 1. /api/courses/share/{slug} — OG meta tags wala HTML page return karta hai
 * (WhatsApp, Facebook, LinkedIn bots ise crawl karte hain)
 * 2. /api/courses/share/{slug}/thumbnail — S3 se thumbnail image proxy karta
 * hai
 * (Yeh stable URL kabhi expire nahi hoti — bots ise directly crawl karti hain)
 *
 * Frontend mein share link format:
 * https://api.skilledup.tech/api/courses/share/{slug}
 * Ya API Gateway se: http://EC2_IP:8080/api/courses/share/{slug}
 */
@RestController
@RequestMapping("/api/courses/share")
public class CourseShareController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * Frontend ka base URL — jahan user redirect hoga share page se
     * Example: https://skilledup.tech
     * .env mein set karo: FRONTEND_BASE_URL=https://skilledup.tech
     */
    @Value("${application.frontend.base-url:https://skilledup.tech}")
    private String frontendBaseUrl;

    /**
     * Aapka public API base URL — yeh OG image URL mein use hoga
     * Example: https://api.skilledup.tech ya http://35.154.236.138:8080
     * .env mein set karo: API_PUBLIC_BASE_URL=https://api.skilledup.tech
     */
    @Value("${application.api.public-base-url:https://api.skilledup.tech}")
    private String apiPublicBaseUrl;

    // -----------------------------------------------------------------------
    // 1) MAIN SHARE PAGE — OG Meta Tags wala HTML
    // -----------------------------------------------------------------------

    // Support for /api/courses/share/id/{id}
    @GetMapping("/id/{id}")
    public ResponseEntity<String> getSharePageById(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        return generateShareHtml(course);
    }

    // Support for /api/courses/share/{slug}
    @GetMapping("/{slug}")
    public ResponseEntity<String> getSharePageBySlug(@PathVariable String slug) {
        // Fallback: Check if slug is actually an ID
        if (slug.matches("\\d+")) {
            return getSharePageById(Long.parseLong(slug));
        }
        Course course = courseRepository.findBySlug(slug).orElse(null);
        return generateShareHtml(course);
    }

    private ResponseEntity<String> generateShareHtml(Course course) {
        if (course == null) {
            // Safer fallback: instead of 500, redirect to home or show clean 404
            return ResponseEntity.status(404).body("<html><body><h1>Course Not Found</h1><script>window.location.href='"
                    + frontendBaseUrl + "'</script></body></html>");
        }

        String title = course.getTitle() != null ? course.getTitle() : "SkilledUp Course";
        String category = course.getCategory() != null && !course.getCategory().isBlank()
                ? course.getCategory()
                : "General";

        // ---- Price text ----
        String priceText;
        if (course.getPrice() != null && course.getPrice() > 0) {
            long rounded = Math.round(course.getPrice());
            priceText = "\u20B9" + String.format("%,d", rounded);
        } else {
            priceText = "Free";
        }

        // ---- Description (short, clean — bots ke liye) ----
        String rawDesc = course.getDescription();
        String shortDesc;
        if (rawDesc != null && !rawDesc.isBlank()) {
            // HTML tags remove karo agar koi tha
            String stripped = rawDesc.replaceAll("<[^>]*>", "").trim();
            shortDesc = stripped.length() > 160 ? stripped.substring(0, 157) + "..." : stripped;
        } else {
            shortDesc = title + " — " + category + " course by SkilledUp.";
        }
        String fullDesc = shortDesc + " | Fee: " + priceText;

        // ---- URLs ----
        String base = clean(frontendBaseUrl, "https://skilledup.tech");
        String apiBase = clean(apiPublicBaseUrl, "https://api.skilledup.tech");
        String safeCategory = category.replace(" ", "-");
        String courseUrl = base + "/courses/" + safeCategory + "/" + course.getSlug();

        // ---- OG Image URL ----
        // Prefer using ID for thumbnail proxy to avoid slug encoding issues
        String thumbnailKey = course.getThumbnailUrl();
        String ogImageUrl;

        if (thumbnailKey != null && !thumbnailKey.isBlank()) {
            if (thumbnailKey.startsWith("http")) {
                ogImageUrl = thumbnailKey;
            } else {
                // Use ID-based proxy for thumbnail for maximum stability
                ogImageUrl = apiBase + "/api/courses/share/thumbnail/" + course.getId();
            }
        } else {
            ogImageUrl = base + "/og-default.png";
        }

        // ---- Discount badge text ----
        String offerText = "";
        if (course.getOriginalPrice() != null && course.getOriginalPrice() > 0 && course.getDiscount() != null
                && course.getDiscount() > 0) {
            offerText = " | " + course.getDiscount() + "% OFF";
        }

        String ogTitle = esc(title) + " | SkilledUp";
        String ogDesc = esc(fullDesc + offerText);

        String html = "<!DOCTYPE html>\n"
                + "<html lang=\"hi\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\" />\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n"
                + "  <title>" + ogTitle + "</title>\n"

                // --- Open Graph (Facebook, WhatsApp, LinkedIn) ---
                + "  <meta property=\"og:type\" content=\"website\" />\n"
                + "  <meta property=\"og:site_name\" content=\"SkilledUp\" />\n"
                + "  <meta property=\"og:url\" content=\"" + esc(courseUrl) + "\" />\n"
                + "  <meta property=\"og:title\" content=\"" + ogTitle + "\" />\n"
                + "  <meta property=\"og:description\" content=\"" + ogDesc + "\" />\n"
                + "  <meta property=\"og:image\" content=\"" + esc(ogImageUrl) + "\" />\n"
                + "  <meta property=\"og:image:width\" content=\"1200\" />\n"
                + "  <meta property=\"og:image:height\" content=\"630\" />\n"
                + "  <meta property=\"og:image:type\" content=\"image/jpeg\" />\n"

                // --- Twitter Card ---
                + "  <meta name=\"twitter:card\" content=\"summary_large_image\" />\n"
                + "  <meta name=\"twitter:site\" content=\"@SkilledUp\" />\n"
                + "  <meta name=\"twitter:title\" content=\"" + ogTitle + "\" />\n"
                + "  <meta name=\"twitter:description\" content=\"" + ogDesc + "\" />\n"
                + "  <meta name=\"twitter:image\" content=\"" + esc(ogImageUrl) + "\" />\n"

                // --- SEO ---
                + "  <meta name=\"description\" content=\"" + ogDesc + "\" />\n"
                + "  <link rel=\"canonical\" href=\"" + esc(courseUrl) + "\" />\n"

                // --- Redirect (humans ke liye, bots redirect nahi follow karte) ---
                + "  <meta http-equiv=\"refresh\" content=\"0; url=" + esc(courseUrl) + "\" />\n"
                + "</head>\n"
                + "<body style=\"font-family:sans-serif;text-align:center;padding:40px;\">\n"
                + "  <h1>" + esc(title) + "</h1>\n"
                + "  <p>" + esc(category) + " &bull; Fee: " + esc(priceText) + "</p>\n"
                + "  <p><a href=\"" + esc(courseUrl) + "\">View Course on SkilledUp</a></p>\n"
                + "</body>\n"
                + "</html>";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name())
                // Crawlers cache karne dete hain 1 ghante tak:
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600").body(html);
    }

    // -----------------------------------------------------------------------
    // 2) THUMBNAIL PROXY — S3 Image directly stream karega (stable URL)
    // -----------------------------------------------------------------------

    // Support for /api/courses/share/thumbnail/{id}
    @GetMapping("/thumbnail/{id}")
    public ResponseEntity<byte[]> getThumbnailById(@PathVariable Long id) throws IOException {
        Course course = courseRepository.findById(id).orElse(null);
        return streamThumbnail(course);
    }

    // Support for legacy /api/courses/share/{slug}/thumbnail
    @GetMapping("/{slug}/thumbnail")
    public ResponseEntity<byte[]> getThumbnailBySlug(@PathVariable String slug) throws IOException {
        if (slug.matches("\\d+")) {
            return getThumbnailById(Long.parseLong(slug));
        }
        Course course = courseRepository.findBySlug(slug).orElse(null);
        return streamThumbnail(course);
    }

    private ResponseEntity<byte[]> streamThumbnail(Course course) throws IOException {

        if (course == null || course.getThumbnailUrl() == null || course.getThumbnailUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String key = course.getThumbnailUrl();

        // Agar already public HTTP URL hai (e.g. CloudFront), redirect karo
        if (key.startsWith("http")) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, key)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .build();
        }

        // S3 se directly stream karo
        try {
            S3Object s3Object = s3Service.getObject(key);
            InputStream is = s3Object.getObjectContent();
            byte[] imageBytes = is.readAllBytes();
            String contentType = s3Object.getObjectMetadata().getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    // 24 ghante cache — bots ke liye best
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /** HTML special characters escape karo OG tag injection se bachne ke liye */
    private String esc(String input) {
        if (input == null)
            return "";
        return input
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** URL clean karo trailing slashes hataane ke liye */
    private String clean(String url, String fallback) {
        if (url == null || url.isBlank())
            return fallback;
        return url.replaceAll("/+$", "");
    }
}
