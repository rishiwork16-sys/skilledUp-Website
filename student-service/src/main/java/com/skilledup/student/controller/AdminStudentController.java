package com.skilledup.student.controller;

import com.skilledup.student.dto.ChangeDomainRequest;
import com.skilledup.student.dto.StudentProfileDTO;
import com.skilledup.student.dto.StudentStatsDTO;
import com.skilledup.student.dto.UpdateStudentStatusRequest;
import com.skilledup.student.model.Student;
import com.skilledup.student.service.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents(
            @RequestParam(required = false) Student.StudentStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminStudentService.getAllStudents(status, city, search));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<StudentProfileDTO> getStudentProfile(@PathVariable Long id) {
        return ResponseEntity.ok(adminStudentService.getStudentProfile(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Student> updateStudentStatus(
            @PathVariable Long id,
            @RequestBody UpdateStudentStatusRequest request) {
        return ResponseEntity.ok(adminStudentService.updateStudentStatus(id, request.getStatus()));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<Student> toggleBlockStudent(@PathVariable Long id) {
        return ResponseEntity.ok(adminStudentService.toggleBlockStudent(id));
    }

    @PutMapping("/{id}/domain")
    public ResponseEntity<Student> changeStudentDomain(
            @PathVariable Long id,
            @RequestBody ChangeDomainRequest request) {
        return ResponseEntity.ok(adminStudentService.changeStudentDomain(id, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<StudentStatsDTO> getStudentStatistics() {
        return ResponseEntity.ok(adminStudentService.getStudentStatistics());
    }
}
