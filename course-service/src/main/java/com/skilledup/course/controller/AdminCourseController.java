package com.skilledup.course.controller;

import com.skilledup.course.entity.Course;
import com.skilledup.course.entity.Module;
import com.skilledup.course.entity.Video;
import com.skilledup.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<?> addModule(@PathVariable Long courseId, @RequestBody Module module) {
        try {
            return ResponseEntity.ok(courseService.addModule(courseId, module));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error adding module: " + e.getMessage());
        }
    }

    @PostMapping("/modules/{moduleId}/videos")
    public ResponseEntity<?> uploadVideo(
            @PathVariable Long moduleId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "duration", defaultValue = "00:00") String duration) throws IOException {
        try {
            return ResponseEntity.ok(courseService.addVideo(moduleId, title, description, file, duration));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Video upload failed"));
        }
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Course> uploadThumbnail(@PathVariable Long id, @RequestParam("file") MultipartFile file)
            throws IOException {
        courseService.uploadThumbnail(id, file);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/{id}/certificate")
    public ResponseEntity<Course> uploadCertificate(@PathVariable Long id, @RequestParam("file") MultipartFile file)
            throws IOException {
        courseService.uploadCertificate(id, file);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/{id}/intro-video")
    public ResponseEntity<Course> uploadIntroVideo(@PathVariable Long id, @RequestParam("file") MultipartFile file)
            throws IOException {
        courseService.uploadIntroVideo(id, file);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/{id}/mentors/image")
    public ResponseEntity<?> uploadMentorImage(@PathVariable Long id, @RequestParam("file") MultipartFile file)
            throws IOException {
        String key = courseService.uploadMentorImage(id, file);
        return ResponseEntity.ok(Map.of(
                "key", key,
                "url", courseService.generatePresignedUrl(key)
        ));
    }

    @PostMapping("/{id}/tools-covered")
    public ResponseEntity<Course> uploadToolsCovered(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "append", defaultValue = "false") boolean append
    ) throws IOException {
        courseService.uploadToolsCovered(id, files, append);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @DeleteMapping("/{id}/tools-covered/{index}")
    public ResponseEntity<Course> deleteToolCoveredAtIndex(@PathVariable Long id, @PathVariable int index) {
        courseService.deleteToolCoveredAtIndex(id, index);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        return ResponseEntity.ok(courseService.updateCourse(id, course));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok().build();
    }
}
