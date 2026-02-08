package com.skilledup.student.controller;

import com.skilledup.student.dto.StudentInquiryRequest;
import com.skilledup.student.dto.StudentInquiryUpdateRequest;
import com.skilledup.student.model.StudentInquiry;
import com.skilledup.student.service.StudentInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StudentInquiryController {
    private final StudentInquiryService inquiryService;

    @PostMapping({"/api/enquiries", "/api/student-inquiries"})
    public ResponseEntity<Map<String, Object>> create(@RequestBody StudentInquiryRequest request) {
        StudentInquiry saved = inquiryService.create(request);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "message", "Inquiry submitted"
        ));
    }

    @GetMapping("/api/admin/enquiries")
    public ResponseEntity<List<StudentInquiry>> list() {
        return ResponseEntity.ok(inquiryService.listAll());
    }

    @PutMapping("/api/admin/enquiries/{id}")
    public ResponseEntity<StudentInquiry> update(@PathVariable Long id, @RequestBody StudentInquiryUpdateRequest request) {
        return ResponseEntity.ok(inquiryService.update(id, request));
    }
}

