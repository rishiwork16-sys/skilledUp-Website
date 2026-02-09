package com.skilledup.career.controller;

import com.skilledup.career.dto.ApiMessage;
import com.skilledup.career.dto.JobApplicationAdminResponse;
import com.skilledup.career.model.Job;
import com.skilledup.career.model.JobApplication;
import com.skilledup.career.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    // --- Public Endpoints ---

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Career Service is running"));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getActiveJobs() {
        return ResponseEntity.ok(careerService.getAllJobs(true));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(careerService.getJob(id));
    }

    @PostMapping("/apply")
    public ResponseEntity<JobApplication> applyJob(
            @RequestParam("data") String applicationData,
            @RequestParam("resume") MultipartFile resume) {
        return ResponseEntity.ok(careerService.applyForJob(applicationData, resume));
    }

    // --- Admin Endpoints ---

    @GetMapping("/admin/jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(careerService.getAllJobs(false));
    }

    @PostMapping("/admin/jobs")
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.ok(careerService.createJob(job));
    }

    @PutMapping("/admin/jobs/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        return ResponseEntity.ok(careerService.updateJob(id, job));
    }

    @DeleteMapping("/admin/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        careerService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/applications")
    public ResponseEntity<List<JobApplicationAdminResponse>> getAllApplications() {
        return ResponseEntity.ok(careerService.getAllApplicationsAdmin());
    }

    @GetMapping("/admin/applications/job/{jobId}")
    public ResponseEntity<List<JobApplication>> getApplicationsByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(careerService.getApplicationsForJob(jobId));
    }

    @GetMapping("/admin/applications/{id}/resume")
    public ResponseEntity<Map<String, String>> getResumeUrl(@PathVariable Long id) {
        String url = careerService.getResumeUrl(id);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
