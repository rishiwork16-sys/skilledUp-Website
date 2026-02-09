package com.skilledup.course.controller;

import com.skilledup.course.dto.ApiMessage;
import com.skilledup.course.entity.Course;
import com.skilledup.course.service.CourseService;
import com.skilledup.course.service.S3Service;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.springframework.beans.factory.annotation.Autowired;
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
        long contentLength = s3Object.getObjectMetadata() != null ? s3Object.getObjectMetadata().getContentLength() : -1L;

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
