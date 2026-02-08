package com.skilledup.career.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilledup.career.dto.JobApplicationAdminResponse;
import com.skilledup.career.dto.JobApplicationRequest;
import com.skilledup.career.model.Job;
import com.skilledup.career.model.JobApplication;
import com.skilledup.career.repository.JobApplicationRepository;
import com.skilledup.career.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    // --- Job Management ---

    public Job createJob(Job job) {
        job.setActive(true);
        return jobRepository.save(job);
    }

    public Job updateJob(Long id, Job jobDetails) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        
        job.setTitle(jobDetails.getTitle());
        job.setLocation(jobDetails.getLocation());
        job.setType(jobDetails.getType());
        job.setExperience(jobDetails.getExperience());
        job.setSalary(jobDetails.getSalary());
        job.setDescription(jobDetails.getDescription());
        job.setRequirements(jobDetails.getRequirements());
        job.setActive(jobDetails.isActive());

        return jobRepository.save(job);
    }

    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    public Job getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public List<Job> getAllJobs(boolean activeOnly) {
        if (activeOnly) {
            return jobRepository.findByActiveTrueOrderByPostedDateDesc();
        }
        return jobRepository.findAllByOrderByPostedDateDesc();
    }

    // --- Application Management ---

    public JobApplication applyForJob(String applicationDataJson, MultipartFile resume) {
        try {
            JobApplicationRequest request = objectMapper.readValue(applicationDataJson, JobApplicationRequest.class);
            
            // Validate Job
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Upload Resume
            String resumeKey = s3Service.uploadFile(resume, "careers/resumes");

            // Map DTO to Entity
            JobApplication application = JobApplication.builder()
                    .job(job)
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .city(request.getCity())
                    .state(request.getState())
                    .pincode(request.getPincode())
                    .workMode(request.getWorkMode())
                    .preferredLocation(request.getPreferredLocation())
                    .currentCompany(request.getCurrentCompany())
                    .totalExperience(request.getTotalExperience())
                    .relevantExperience(request.getRelevantExperience())
                    .noticePeriod(request.getNoticePeriod())
                    .linkedInUrl(request.getLinkedInUrl())
                    .githubUrl(request.getGithubUrl())
                    .otherPortfolioUrl(request.getOtherPortfolioUrl())
                    .additionalInformation(request.getAdditionalInformation())
                    .resumeKey(resumeKey)
                    .build();

            return applicationRepository.save(application);
        } catch (Exception e) {
            throw new RuntimeException("Error processing application", e);
        }
    }

    @Transactional(readOnly = true)
    public List<JobApplicationAdminResponse> getAllApplicationsAdmin() {
        List<JobApplication> applications = applicationRepository.findAll();

        return applications.stream()
                .map(app -> JobApplicationAdminResponse.builder()
                        .id(app.getId())
                        .jobId(app.getJob() != null ? app.getJob().getId() : null)
                        .jobTitle(app.getJob() != null ? app.getJob().getTitle() : null)
                        .fullName(app.getFullName())
                        .email(app.getEmail())
                        .phone(app.getPhone())
                        .currentCompany(app.getCurrentCompany())
                        .city(app.getCity())
                        .state(app.getState())
                        .pincode(app.getPincode())
                        .workMode(app.getWorkMode())
                        .preferredLocation(app.getPreferredLocation())
                        .totalExperience(app.getTotalExperience())
                        .relevantExperience(app.getRelevantExperience())
                        .noticePeriod(app.getNoticePeriod())
                        .appliedDate(app.getAppliedDate())
                        .build())
                .toList();
    }

    public List<JobApplication> getApplicationsForJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    public List<JobApplication> getAllApplications() {
        return applicationRepository.findAll();
    }

    public String getResumeUrl(Long applicationId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        return s3Service.generatePresignedUrl(application.getResumeKey());
    }
}
