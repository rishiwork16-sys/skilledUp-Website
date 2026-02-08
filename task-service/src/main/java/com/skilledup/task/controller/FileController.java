package com.skilledup.task.controller;

import com.skilledup.task.service.S3Service;
import com.skilledup.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;
    private final TaskService taskService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = s3Service.uploadFile(file);
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-from-url")
    public ResponseEntity<Map<String, String>> uploadFromUrl(@RequestParam("url") String url) {
        try {
            String fileUrl = s3Service.uploadFromUrl(url);
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload from URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, String>> getSignedUrl(@RequestParam("fileUrl") String fileUrl) {
        try {
            String signedUrl = s3Service.generateSignedUrl(fileUrl);
            Map<String, String> response = new HashMap<>();
            response.put("signedUrl", signedUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate signed URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFile(
            @RequestParam("taskId") Long taskId,
            @RequestParam("fileType") String fileType) {
        try {
            taskService.deleteTaskFile(taskId, fileType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
