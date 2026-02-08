package com.skilledup.student.controller;

import com.skilledup.student.model.InternshipType;
import com.skilledup.student.repository.InternshipTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internship-types")
@RequiredArgsConstructor
public class InternshipTypeController {

    private final InternshipTypeRepository internshipTypeRepository;

    @GetMapping
    public ResponseEntity<List<InternshipType>> getAllTypes() {
        return ResponseEntity.ok(internshipTypeRepository.findByActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternshipType> getType(@PathVariable Long id) {
        return ResponseEntity.ok(
                internshipTypeRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Internship type not found")));
    }

    @PostMapping
    public ResponseEntity<InternshipType> createType(@RequestBody InternshipType internshipType) {
        return ResponseEntity.ok(internshipTypeRepository.save(internshipType));
    }
}
