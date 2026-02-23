package com.skilledup.course.controller;

import com.skilledup.course.entity.Course;
import com.skilledup.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/courses/share")
public class CourseShareController {

    @Autowired
    private CourseService courseService;

    @Value("${application.frontend.base-url:https://skilledup.tech}")
    private String frontendBaseUrl;

    @GetMapping("/{slug}")
    public ResponseEntity<String> getSharePage(@PathVariable String slug) {
        Course course = courseService.getCourseBySlug(slug);

        String category = course.getCategory() != null && !course.getCategory().isBlank()
                ? course.getCategory()
                : "General";

        String title = course.getTitle() != null ? course.getTitle() : "SkilledUp Course";

        String priceText;
        if (course.getPrice() != null) {
            long rounded = Math.round(course.getPrice());
            priceText = "Fee: \u20B9" + String.format("%,d", rounded);
        } else {
            priceText = "Fee: Contact for details";
        }

        String description;
        if (course.getDescription() != null && !course.getDescription().isBlank()) {
            description = course.getDescription();
        } else {
            description = title + " by SkilledUp. " + priceText;
        }

        String base = frontendBaseUrl != null && !frontendBaseUrl.isBlank()
                ? frontendBaseUrl.replaceAll("/+$", "")
                : "https://skilledup.tech";

        String safeCategory = category.replace(" ", "-");

        String courseUrl = base + "/courses/" + safeCategory + "/" + course.getSlug();

        String imageUrl = course.getThumbnailUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = base + "/images/skilledup-og-1200x630.png";
        } else if (!imageUrl.startsWith("http")) {
            if (!imageUrl.startsWith("/")) {
                imageUrl = "/" + imageUrl;
            }
            imageUrl = base + imageUrl;
        }

        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <title>" + esc(title) + " | SkilledUp</title>\n" +
                "  <meta property=\"og:title\" content=\"" + esc(title) + " | SkilledUp\" />\n" +
                "  <meta property=\"og:description\" content=\"" + esc(description + " " + priceText) + "\" />\n" +
                "  <meta property=\"og:image\" content=\"" + esc(imageUrl) + "\" />\n" +
                "  <meta property=\"og:type\" content=\"website\" />\n" +
                "  <meta property=\"og:url\" content=\"" + esc(courseUrl) + "\" />\n" +
                "  <meta name=\"twitter:card\" content=\"summary_large_image\" />\n" +
                "  <meta name=\"twitter:image\" content=\"" + esc(imageUrl) + "\" />\n" +
                "  <meta http-equiv=\"refresh\" content=\"0; url=" + esc(courseUrl) + "\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "Redirecting to course...\n" +
                "</body>\n" +
                "</html>";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name())
                .body(html);
    }

    private String esc(String input) {
        if (input == null) {
            return "";
        }
        String escaped = input.replace("&", "&amp;");
        escaped = escaped.replace("\"", "&quot;");
        return escaped;
    }
}

