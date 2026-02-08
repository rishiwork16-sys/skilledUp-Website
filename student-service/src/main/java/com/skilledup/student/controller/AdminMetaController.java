package com.skilledup.student.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMetaController {

    @GetMapping("/backgrounds")
    public ResponseEntity<List<Map<String, String>>> getBackgrounds() {
        return ResponseEntity.ok(List.of(
                Map.of("name", "Student"),
                Map.of("name", "Working Professional"),
                Map.of("name", "Fresher"),
                Map.of("name", "Career Switcher")
        ));
    }

    @GetMapping("/states")
    public ResponseEntity<List<Map<String, String>>> getStates() {
        return ResponseEntity.ok(List.of(
                Map.of("name", "Uttar Pradesh"),
                Map.of("name", "Delhi"),
                Map.of("name", "Haryana"),
                Map.of("name", "Bihar"),
                Map.of("name", "Madhya Pradesh"),
                Map.of("name", "Rajasthan"),
                Map.of("name", "Maharashtra"),
                Map.of("name", "Karnataka"),
                Map.of("name", "Gujarat"),
                Map.of("name", "West Bengal"),
                Map.of("name", "Punjab"),
                Map.of("name", "Other")
        ));
    }
}

