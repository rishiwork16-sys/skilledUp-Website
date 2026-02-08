package com.skilledup.student.controller;

import com.skilledup.student.dto.ApiMessage;
import com.skilledup.student.dto.EnrollmentRequest;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.InternshipCategory;
import com.skilledup.student.model.Student;
import com.skilledup.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/enroll")
    public ResponseEntity<InternshipEnrollment> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.ok(studentService.enroll(request));
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<List<InternshipEnrollment>> getMyEnrollments(@RequestParam Long userId) {
        return ResponseEntity.ok(studentService.getMyEnrollments(userId));
    }

    @GetMapping("/my-enrollments-by-phone")
    public ResponseEntity<List<InternshipEnrollment>> getMyEnrollmentsByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(studentService.getMyEnrollmentsByPhone(phone));
    }

    @GetMapping("/profile-by-phone")
    public ResponseEntity<Student> getProfileByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(studentService.getStudentProfileByPhone(phone));
    }

    @GetMapping("/all-enrollments")
    public ResponseEntity<List<InternshipEnrollment>> getAllEnrollments() {
        return ResponseEntity.ok(studentService.getAllEnrollments());
    }

    public ResponseEntity<List<InternshipCategory>> getInternshipTypes() {
        return ResponseEntity.ok(studentService.getAllInternshipTypes());
    }

    @GetMapping("/search/active-by-domain")
    public ResponseEntity<List<Long>> getActiveStudentsByDomain(@RequestParam String domain) {
        return ResponseEntity.ok(studentService.getActiveStudentsByDomain(domain));
    }

    @GetMapping("/profile")
    public ResponseEntity<Student> getProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(studentService.getStudentProfile(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Student Service is running"));
    }

    @GetMapping("/tasks")
    public ResponseEntity<Object> getMyTasks(@RequestParam("studentId") Long studentId) {
        return ResponseEntity.ok(studentService.getMyTasks(studentId));
    }

    @PostMapping("/tasks/submit")
    public ResponseEntity<Object> submitTask(@RequestBody Object submissionRequest) {
        return ResponseEntity.ok(studentService.submitTask(submissionRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudent(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id,
            @RequestBody com.skilledup.student.dto.RegisterRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessage> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(new ApiMessage("Student and associated User deleted successfully"));
    }

    @PostMapping("/enrollments/{enrollmentId}/offer-letter")
    public ResponseEntity<String> regenerateOfferLetter(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(studentService.regenerateOfferLetter(enrollmentId));
    }

    @PostMapping("/exit")
    public ResponseEntity<String> exitInternship(@RequestBody com.skilledup.student.dto.ExitRequest request) {
        return ResponseEntity.ok(studentService.exitInternship(request));
    }
}