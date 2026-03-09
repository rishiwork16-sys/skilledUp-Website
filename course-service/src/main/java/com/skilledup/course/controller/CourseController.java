package com.skilledup.course.controller;

import com.skilledup.course.dto.ApiMessage;
import com.skilledup.course.entity.Course;
import com.skilledup.course.service.CourseService;
import com.skilledup.course.service.S3Service;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Course Service is running"));
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Course> getCourseBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(courseService.getCourseBySlug(slug));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/share/{slug}")
    public ResponseEntity<String> getSharePage(
            @PathVariable String slug,
            @Value("${application.frontend.base-url:https://skilledup.tech}") String frontendBaseUrl) {
        try {
            com.skilledup.course.entity.Course course = courseService.getCourseBySlug(slug);

            String title = course.getTitle() != null ? escapeHtml(course.getTitle()) : "SkilledUp Course";
            String description = course.getDescription() != null
                    ? escapeHtml(course.getDescription().substring(0, Math.min(200, course.getDescription().length())))
                    : "Advance your career with SkilledUp";
            String imageUrl = course.getThumbnailUrl() != null ? course.getThumbnailUrl() : "";
            String priceText = course.getPrice() != null ? "₹" + course.getPrice().longValue() : "";
            String category = course.getCategory() != null
                    ? course.getCategory().replace(" ", "%20")
                    : "courses";
            String courseUrl = frontendBaseUrl + "/courses/" + category + "/" + slug;
            String ogDesc = (priceText.isEmpty() ? "" : priceText + " | ")
                    + (course.getDuration() != null ? course.getDuration() + " | " : "")
                    + description;

            String html = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
                    + "  <meta charset=\"UTF-8\"/>\n"
                    + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n"
                    + "  <title>" + title + " | SkilledUp</title>\n"
                    + "  <meta name=\"description\" content=\"" + escapeHtml(ogDesc) + "\"/>\n"
                    // Open Graph
                    + "  <meta property=\"og:type\" content=\"website\"/>\n"
                    + "  <meta property=\"og:site_name\" content=\"SkilledUp\"/>\n"
                    + "  <meta property=\"og:title\" content=\"" + title + "\"/>\n"
                    + "  <meta property=\"og:description\" content=\"" + escapeHtml(ogDesc) + "\"/>\n"
                    + "  <meta property=\"og:image\" content=\"" + imageUrl + "\"/>\n"
                    + "  <meta property=\"og:image:width\" content=\"1200\"/>\n"
                    + "  <meta property=\"og:image:height\" content=\"630\"/>\n"
                    + "  <meta property=\"og:url\" content=\"" + courseUrl + "\"/>\n"
                    // Twitter Card
                    + "  <meta name=\"twitter:card\" content=\"summary_large_image\"/>\n"
                    + "  <meta name=\"twitter:title\" content=\"" + title + "\"/>\n"
                    + "  <meta name=\"twitter:description\" content=\"" + escapeHtml(ogDesc) + "\"/>\n"
                    + "  <meta name=\"twitter:image\" content=\"" + imageUrl + "\"/>\n"
                    // Redirect to frontend
                    + "  <meta http-equiv=\"refresh\" content=\"0; url=" + courseUrl + "\"/>\n"
                    + "</head>\n<body>\n"
                    + "  <p>Redirecting to <a href=\"" + courseUrl + "\">" + title + "</a>...</p>\n"
                    + "</body>\n</html>";

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .header("Cache-Control", "public, max-age=3600")
                    .body(html);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @GetMapping("/play/{videoId}")
    public ResponseEntity<?> getStreamUrl(@PathVariable Long videoId, @RequestParam Long userId) {
        // Implementation: Ideally this calls payment-service /api/payments/access
        // Since we are in the course-service, we just log the access attempt for now.

        System.out.println("User " + userId + " attempting to stream video " + videoId);

        // Return pre-signed S3 URL from service
        return ResponseEntity.ok(courseService.getVideoUrl(videoId));
    }

    @Autowired
    private S3Service s3Service;

    @GetMapping(value = "/hls/{videoId}/master.m3u8")
    public ResponseEntity<StreamingResponseBody> getHlsMaster(@PathVariable Long videoId) {
        return streamHlsAsset(videoId, "master.m3u8");
    }

    @GetMapping(value = "/hls/{videoId}/{stream}/playlist.m3u8")
    public ResponseEntity<StreamingResponseBody> getHlsVariantPlaylist(
            @PathVariable Long videoId,
            @PathVariable String stream) {
        return streamHlsAsset(videoId, stream + "/playlist.m3u8");
    }

    @GetMapping(value = "/hls/{videoId}/{stream}/{fileName:.+}")
    public ResponseEntity<StreamingResponseBody> getHlsStreamAsset(
            @PathVariable Long videoId,
            @PathVariable String stream,
            @PathVariable String fileName) {
        return streamHlsAsset(videoId, stream + "/" + fileName);
    }

    private ResponseEntity<StreamingResponseBody> streamHlsAsset(Long videoId, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        String baseKey = courseService.getVideoS3BaseKey(videoId);
        String s3Key = baseKey + "/" + normalized;

        S3Object s3Object;
        try {
            s3Object = s3Service.getObject(s3Key);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
        long contentLength = s3Object.getObjectMetadata() != null ? s3Object.getObjectMetadata().getContentLength()
                : -1L;

        String contentType = detectContentType(normalized);
        StreamingResponseBody body = outputStream -> {
            try (var in = s3Object.getObjectContent()) {
                in.transferTo(outputStream);
            }
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(contentType));
        if (contentLength >= 0) {
            builder.contentLength(contentLength);
        }
        return builder.body(body);
    }

    private String detectContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (lower.endsWith(".ts")) {
            return "video/mp2t";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
