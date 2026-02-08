package com.skilledup.student.controller;

import com.skilledup.student.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String key = s3Service.uploadFile(file);
        String url = s3Service.generatePresignedUrl(key);
        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("url", url);
        return ResponseEntity.ok(response);
    }
}
