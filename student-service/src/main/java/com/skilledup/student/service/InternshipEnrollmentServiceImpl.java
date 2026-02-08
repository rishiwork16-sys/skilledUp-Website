package com.skilledup.student.service;

import com.skilledup.student.client.AuthClient;
import com.skilledup.student.client.NotificationClient;
import com.skilledup.student.dto.AuthResponse;
import com.skilledup.student.dto.EmailRequest;
import com.skilledup.student.dto.EnrollmentRequest;
import com.skilledup.student.dto.RegisterRequest;
import com.skilledup.student.dto.EnrollmentResponse;
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

import java.io.File;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipEnrollmentServiceImpl implements InternshipEnrollmentService {

        private final InternshipEnrollmentRepository enrollmentRepository;
        private final StudentRepository studentRepository;
        private final com.skilledup.student.repository.InternshipCategoryRepository internshipCategoryRepository;
        private final AuthClient authClient;
        private final NotificationClient notificationClient;
        private final OfferLetterGeneratorService offerLetterGeneratorService;
        private final S3Service s3Service;
        private final com.skilledup.student.client.TaskClient taskClient;

        @Override
        @Transactional
        public EnrollmentResponse enrollPublic(EnrollmentRequest request) {
                log.info("Starting public enrollment for email: {}", request.getEmail());

                // 1. Fetch Internship Category (Source of Truth)
                InternshipCategory category = internshipCategoryRepository.findById(request.getInternshipTypeId())
                                .orElseThrow(() -> new RuntimeException("Internship Category not found"));

                // 2. Generate Password
                String password = "SKILL@" + (int) (Math.random() * 100000);

                // 3. Create User in Auth Service
                AuthResponse authResponse = null;
                try {
                        RegisterRequest registerRequest = RegisterRequest.builder()
                                        .name(request.getName())
                                        .email(request.getEmail())
                                        .mobile(request.getPhone())
                                        .password(password)
                                        .emailVerified(true)
                                        .mobileVerified(true)
                                        .build();

                        authResponse = authClient.register(registerRequest);
                        log.info("User registered in Auth Service: {}", authResponse.getEmail());
                } catch (Exception e) {
                        log.warn("Registration failed (possibly already exists), trying to fetch user: {}",
                                        e.getMessage());
                        try {
                                // If registration fails, try to get existing user
                                authResponse = authClient.getUserByEmail(request.getEmail());
                                log.info("Found existing user: {}", authResponse.getEmail());
                        } catch (Exception ex) {
                                // If both fail, throw original error
                                log.error("Failed to register OR fetch user", ex);
                                throw new RuntimeException("User registration failed: " + e.getMessage());
                        }
                }

                // 4. Create/Get Student
                Student student = studentRepository.findByUserId(authResponse.getId())
                                .orElse(null);

                if (student == null) {
                        student = Student.builder()
                                        .userId(authResponse.getId())
                                        .name(request.getName())
                                        .email(request.getEmail())
                                        .phone(request.getPhone())
                                        .address(request.getAddress())
                                        .city(request.getCity())
                                        .state(request.getState())
                                        .country(request.getCountry())
                                        .pincode(request.getPincode())
                                        .status(Student.StudentStatus.ACTIVE)
                                        .build();
                        student = studentRepository.save(student);
                        log.info("Created new student profile for user: {}", authResponse.getId());
                } else {
                        log.info("Found existing student profile: {}", student.getId());
                        // Optional update if needed
                        if (request.getCity() != null)
                                student.setCity(request.getCity());
                        if (request.getPhone() != null)
                                student.setPhone(request.getPhone());
                        if (request.getAddress() != null)
                                student.setAddress(request.getAddress());
                        if (request.getState() != null)
                                student.setState(request.getState());
                        if (request.getCountry() != null)
                                student.setCountry(request.getCountry());
                        if (request.getPincode() != null)
                                student.setPincode(request.getPincode());

                        studentRepository.save(student);
                }

                // 5. Check for Existing Active Enrollment
                Optional<InternshipEnrollment> existingEnrollment = enrollmentRepository
                                .findByStudentAndInternshipCategoryAndStatus(
                                                student,
                                                category,
                                                InternshipEnrollment.EnrollmentStatus.ACTIVE);

                if (existingEnrollment.isPresent()) {
                        log.info("Student {} is already enrolled in {}", student.getEmail(), category.getTitle());
                        return EnrollmentResponse.builder()
                                        .enrollment(existingEnrollment.get())
                                        .token(authResponse.getToken())
                                        .build();
                }

                // 6. Create New Enrollment
                InternshipEnrollment enrollment = InternshipEnrollment.builder()
                                .student(student)
                                .internshipCategory(category)
                                .duration(request.getDuration())
                                .startDate(LocalDate.now())
                                .status(InternshipEnrollment.EnrollmentStatus.ACTIVE)
                                .enrolledAt(Instant.now())
                                .progress(0)
                                .build();

                enrollment = enrollmentRepository.save(enrollment);

                // 5.5 Initialize Tasks in Task Service
                try {
                        taskClient.initializeTasks(student.getId(), category.getTitle());
                        log.info("Initialized tasks for student {} in domain {}", student.getId(), category.getTitle());
                } catch (Exception e) {
                        log.error("Failed to initialize tasks", e);
                        // Don't fail enrollment if task init fails, can be done later or by admin
                }

                // 6. Generate Offer Letter
                try {
                        File pptx = offerLetterGeneratorService.generateOfferLetter(enrollment);
                        String s3Path = "offer-letters/" + request.getEmail() + "/offer-letter.pptx";
                        String s3Key = s3Service.uploadFile(s3Path, pptx);
                        String signedUrl = s3Service.generatePresignedUrl(s3Key);

                        enrollment.setOfferLetterUrl(s3Key);
                        enrollmentRepository.save(enrollment);

                        // 7. Send Emails
                        EmailRequest accountEmail = EmailRequest.builder()
                                        .recipient(request.getEmail())
                                        .type("WELCOME")
                                        .subject("Welcome to Skilledup - Account Created")
                                        .body("<p>Your account has been created.</p><p>Email: " + request.getEmail()
                                                        + "</p><p>Password: "
                                                        + password + "</p>")
                                        .build();
                        notificationClient.sendEmail(accountEmail);

                        EmailRequest offerEmail = EmailRequest.builder()
                                        .recipient(request.getEmail())
                                        .type("OFFER_LETTER")
                                        .subject("Your Offer Letter from Skilledup")
                                        .attachmentUrl(signedUrl)
                                        .build();
                        notificationClient.sendEmail(offerEmail);

                        if (pptx != null && pptx.exists()) {
                                pptx.delete();
                        }

                } catch (Exception e) {
                        log.error("Error during offer letter generation/sending", e);
                }

                return EnrollmentResponse.builder()
                                .enrollment(enrollment)
                                .token(authResponse.getToken())
                                .build();
        }

        @Override
        public String regenerateOfferLetter(Long enrollmentId) {
                InternshipEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

                Student student = enrollment.getStudent();
                log.info("Regenerating offer letter for student: {}", student.getEmail());

                try {
                        File pptx = offerLetterGeneratorService.generateOfferLetter(enrollment);
                        String s3Path = "offer-letters/" + student.getEmail() + "/offer-letter.pptx";
                        String s3Key = s3Service.uploadFile(s3Path, pptx);
                        String signedUrl = s3Service.generatePresignedUrl(s3Key);

                        enrollment.setOfferLetterUrl(s3Key);
                        enrollmentRepository.save(enrollment);

                        // Send Email
                        EmailRequest offerEmail = EmailRequest.builder()
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
}
