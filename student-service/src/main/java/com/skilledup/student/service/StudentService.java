package com.skilledup.student.service;

import com.skilledup.student.client.AuthClient;
import com.skilledup.student.client.NotificationClient;
import com.skilledup.student.dto.AuthResponse;
import com.skilledup.student.dto.EnrollmentRequest;
import com.skilledup.student.dto.RegisterRequest;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.InternshipCategory;
import com.skilledup.student.model.Student;
import com.skilledup.student.repository.InternshipEnrollmentRepository;
import com.skilledup.student.repository.InternshipCategoryRepository;
import com.skilledup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final InternshipCategoryRepository internshipCategoryRepository;
    private final InternshipEnrollmentRepository enrollmentRepository;
    private final AuthClient authClient;
    private final com.skilledup.student.client.TaskClient taskClient;
    private final NotificationClient notificationClient;
    private final OfferLetterGeneratorService offerLetterGeneratorService;
    private final S3Service s3Service;
    private final com.skilledup.student.client.CertificateClient certificateClient;

    @Transactional
    public InternshipEnrollment enroll(EnrollmentRequest request) {

        // 1. Ensure User exists in Auth Service
        Long userId = request.getUserId();
        if (userId == null) {
            // Check if user exists by email
            try {
                AuthResponse existingUser = authClient.getUserByEmail(request.getEmail());
                userId = existingUser.getId();
            } catch (Exception e) {
                // Register new user
                String password = "SKILL@" + (int) (Math.random() * 100000);
                try {
                    RegisterRequest registerRequest = RegisterRequest.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .mobile(request.getPhone())
                            .password(password)
                            .emailVerified(true)
                            .mobileVerified(true)
                            .build();
                    AuthResponse newUser = authClient.register(registerRequest);
                    userId = newUser.getId();
                    log.info("Registered new user in Auth Service via StudentService: {}", newUser.getEmail());
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to register user in Auth Service: " + ex.getMessage());
                }
            }
        }

        // 2. Find or create student
        final Long finalUserId = userId;
        Student student = studentRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Student newStudent = Student.builder()
                            .userId(finalUserId)
                            .name(request.getName())
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .city(request.getCity())
                            .build();
                    return studentRepository.save(newStudent);
                });

        // 3. Find internship category
        InternshipCategory internshipCategory = internshipCategoryRepository.findById(request.getInternshipTypeId())
                .orElseThrow(() -> new RuntimeException("Internship category not found"));

        // 4. Calculate start date (next Monday)
        LocalDate startDate = getNextMonday();
        LocalDate endDate = startDate.plusWeeks(request.getDuration());

        // 5. Create enrollment
        InternshipEnrollment enrollment = InternshipEnrollment.builder()
                .student(student)
                .internshipCategory(internshipCategory)
                .duration(request.getDuration())
                .startDate(startDate)
                .endDate(endDate)
                .status(InternshipEnrollment.EnrollmentStatus.ACTIVE)
                .progress(0)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        enrollment = enrollmentRepository.save(enrollment);

        // Initialize Tasks
        try {
            taskClient.initializeTasks(student.getId(), internshipCategory.getTitle());
            log.info("Initialized tasks for student {} in domain {}", student.getId(), internshipCategory.getTitle());
        } catch (Exception e) {
            log.error("Failed to initialize tasks", e);
        }

        // Generate Offer Letter
        try {
            java.io.File pptx = offerLetterGeneratorService.generateOfferLetter(enrollment);
            String s3Path = "offer-letters/" + request.getEmail() + "/offer-letter.pptx";
            String s3Key = s3Service.uploadFile(s3Path, pptx);
            String signedUrl = s3Service.generatePresignedUrl(s3Key);

            enrollment.setOfferLetterUrl(s3Key);
            enrollmentRepository.save(enrollment);

            // Send Email
            com.skilledup.student.dto.EmailRequest offerEmail = com.skilledup.student.dto.EmailRequest.builder()
                    .recipient(request.getEmail())
                    .type("OFFER_LETTER")
                    .subject("Your Offer Letter from Skilledup")
                    .attachmentUrl(signedUrl)
                    .build();
            notificationClient.sendEmail(offerEmail);

            log.info("Generated and sent offer letter to {}", request.getEmail());
            if (pptx != null && pptx.exists()) {
                pptx.delete();
            }

        } catch (Exception e) {
            log.error("Error during offer letter generation/sending", e);
        }

        log.info("Student {} enrolled in {} internship", student.getName(), internshipCategory.getTitle());

        return enrollment;
    }

    public String regenerateOfferLetter(Long enrollmentId) {
        InternshipEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Student student = enrollment.getStudent();
        log.info("Regenerating offer letter for student: {}", student.getEmail());

        try {
            java.io.File pptx = offerLetterGeneratorService.generateOfferLetter(enrollment);
            String s3Path = "offer-letters/" + student.getEmail() + "/offer-letter.pptx";
            String s3Key = s3Service.uploadFile(s3Path, pptx);
            String signedUrl = s3Service.generatePresignedUrl(s3Key);

            enrollment.setOfferLetterUrl(s3Key);
            enrollmentRepository.save(enrollment);

            // Send Email
            com.skilledup.student.dto.EmailRequest offerEmail = com.skilledup.student.dto.EmailRequest.builder()
                    .recipient(student.getEmail())
                    .type("OFFER_LETTER")
                    .subject("Your Offer Letter from Skilledup (Regenerated)")
                    .attachmentUrl(signedUrl)
                    .build();
            notificationClient.sendEmail(offerEmail);

            if (pptx != null && pptx.exists()) {
                pptx.delete();
            }

            return signedUrl;

        } catch (Exception e) {
            log.error("Failed to regenerate offer letter", e);
            throw new RuntimeException("Regeneration failed: " + e.getMessage());
        }
    }

    public List<InternshipEnrollment> getMyEnrollments(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<InternshipEnrollment> enrollments = enrollmentRepository.findByStudent(student);
        resolveEnrollmentUrls(enrollments);
        return enrollments;
    }

    public List<InternshipEnrollment> getMyEnrollmentsByPhone(String phone) {
        List<InternshipEnrollment> enrollments = studentRepository.findByPhone(phone)
                .map(enrollmentRepository::findByStudent)
                .orElse(java.util.Collections.emptyList());
        resolveEnrollmentUrls(enrollments);
        return enrollments;
    }

    public Student getStudentProfileByPhone(String phone) {
        return studentRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Student not found with phone: " + phone));
    }

    public List<InternshipEnrollment> getAllEnrollments() {
        List<InternshipEnrollment> enrollments = enrollmentRepository.findAll();
        resolveEnrollmentUrls(enrollments);
        return enrollments;
    }

    public Student getStudentProfile(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    public Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    public List<InternshipCategory> getAllInternshipTypes() {
        return internshipCategoryRepository.findByActiveTrue();
    }

    public List<Long> getActiveStudentsByDomain(String domain) {
        return enrollmentRepository.findByInternshipCategory_TitleAndStatus(
                domain,
                InternshipEnrollment.EnrollmentStatus.ACTIVE).stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .collect(java.util.stream.Collectors.toList());
    }

    public Object getMyTasks(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return taskClient.getMyTasks(student.getId());
    }

    public Object submitTask(Object submissionRequest) {
        return taskClient.submitTask(submissionRequest);
    }

    private LocalDate getNextMonday() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.MONDAY) {
            return today;
        } else {
            int daysUntilMonday = DayOfWeek.MONDAY.getValue() - dayOfWeek.getValue();
            if (daysUntilMonday < 0) {
                daysUntilMonday += 7;
            }
            return today.plusDays(daysUntilMonday);
        }
    }

    private void resolveEnrollmentUrls(List<InternshipEnrollment> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) return;
        for (InternshipEnrollment enrollment : enrollments) {
            if (enrollment == null) continue;
            String keyOrUrl = enrollment.getOfferLetterUrl();
            if (keyOrUrl != null && !keyOrUrl.isBlank()) {
                enrollment.setOfferLetterUrl(s3Service.generatePresignedUrl(keyOrUrl));
            }
        }
    }

    @Transactional
    public Student updateStudent(Long id, com.skilledup.student.dto.RegisterRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Update local student
        if (request.getName() != null)
            student.setName(request.getName());
        if (request.getMobile() != null)
            student.setPhone(request.getMobile());
        if (request.getCity() != null)
            student.setCity(request.getCity());

        student = studentRepository.save(student);

        // Sync with Auth Service
        try {
            authClient.updateUser(student.getUserId(), request);
            log.info("Synced updated student {} to Auth Service", id);
        } catch (Exception e) {
            log.error("Failed to sync student update to Auth Service", e);
            // Decide if we should rollback or just log. For now, we log.
        }

        return student;
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Long userId = student.getUserId();

        // Delete enrollments? (Cascade normally handles this but explicit if needed)
        // enrollmentRepository.deleteAllByStudent(student);

        studentRepository.delete(student);

        // Sync with Auth Service
        try {
            authClient.deleteUser(userId);
            log.info("Synced deleted student {} to Auth Service", id);
        } catch (Exception e) {
            log.error("Failed to sync student delete to Auth Service", e);
        }
    }

    public void seedInternshipTypes() {
        InternshipCategory web = InternshipCategory.builder().title("Web Development").description("Web Dev")
                .active(true).build();
        InternshipCategory app = InternshipCategory.builder().title("App Development").description("App Dev")
                .active(true).build();
        internshipCategoryRepository.saveAll(List.of(web, app));
    }

    @Transactional
    public String exitInternship(com.skilledup.student.dto.ExitRequest request) {
        log.info("Processing exit for enrollment ID: {}", request.getEnrollmentId());

        InternshipEnrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        // Update Enrollment Status
        enrollment.setStatus(InternshipEnrollment.EnrollmentStatus.COMPLETED);

        LocalDate endDate = LocalDate.now();
        if (request.getLastWorkingDay() != null && !request.getLastWorkingDay().isEmpty()) {
            try {
                // Parse yyyy-MM-dd
                endDate = LocalDate.parse(request.getLastWorkingDay());
            } catch (Exception e) {
                log.warn("Invalid date format in exit request: {}, using current date.", request.getLastWorkingDay());
            }
        }
        enrollment.setEndDate(endDate);

        // Generate Certificate
        try {
            com.skilledup.student.dto.LORGenerationRequest certRequest = com.skilledup.student.dto.LORGenerationRequest
                    .builder()
                    .studentName(enrollment.getStudent().getName())
                    .email(enrollment.getStudent().getEmail())
                    .domain(enrollment.getInternshipCategory().getTitle())
                    .startDate(enrollment.getStartDate())
                    .endDate(enrollment.getEndDate())
                    .duration(enrollment.getDuration())
                    .lorId("CERT-" + System.currentTimeMillis()) // Using CERT prefix
                    .build();

            String certUrl = certificateClient.generateInternshipCertificate(certRequest);
            enrollment.setCertificateUrl(certUrl);
            log.info("Certificate generated and saved: {}", certUrl);

        } catch (Exception e) {
            log.error("Failed to generate certificate during exit: {}", e.getMessage());
        }

        enrollmentRepository.save(enrollment);
        return enrollment.getCertificateUrl();
    }
}
