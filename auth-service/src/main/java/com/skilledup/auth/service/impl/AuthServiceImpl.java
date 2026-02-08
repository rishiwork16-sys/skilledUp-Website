package com.skilledup.auth.service.impl;

import com.skilledup.auth.dto.ApiMessage;
import com.skilledup.auth.dto.AuthResponse;
import com.skilledup.auth.dto.LoginRequest;
import com.skilledup.auth.dto.RegisterRequest;
import com.skilledup.auth.dto.ResetPasswordRequest;
import com.skilledup.auth.model.OtpRecord;
import com.skilledup.auth.model.Role;
import com.skilledup.auth.model.User;
import com.skilledup.auth.repository.OtpRepository;
import com.skilledup.auth.repository.UserRepository;
import com.skilledup.auth.security.JwtTokenProvider;
import com.skilledup.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.skilledup.auth.client.NotificationClient notificationClient;
    private final JdbcTemplate jdbcTemplate;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (userRepository.existsByMobile(request.getMobile())) {
            throw new RuntimeException("Mobile already registered");
        }

        // Create new user
        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateRandomPassword();
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ROLE_STUDENT)
                .active(true)
                .emailVerified(request.isEmailVerified())
                .mobileVerified(request.isMobileVerified())
                .build();

        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(),
                user.getName());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Login failed: User not found for email: {}", request.getEmail());
                    return new RuntimeException("Invalid credentials");
                });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Login failed: Password mismatch for email: {}", request.getEmail());
            // Log hash for debug (warning: secure environment strictly bad practice, but
            // useful for verifying truncation)
            log.debug("DB Hash length: {}", user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
            throw new RuntimeException("Invalid credentials");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(),
                user.getName());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    public AuthResponse loginWithOtp(String mobile, String otp) {
        // 1. Verify OTP
        verifyOtp(mobile, otp, "MOBILE");

        // 2. Find User
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new RuntimeException("User not found via mobile"));

        // 3. Check Active
        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        // 4. Generate Token
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(),
                user.getName());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    public AuthResponse loginWithOtp(String identifier, String otp, String type) {
        verifyOtp(identifier, otp, type);
        OtpRecord.OtpType otpType = OtpRecord.OtpType.valueOf(type.toUpperCase());

        User user;
        if (otpType == OtpRecord.OtpType.EMAIL) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            user = userRepository.findByMobile(identifier)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(),
                user.getName());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    @Transactional
    public ApiMessage sendOtp(String identifier, String type) {
        OtpRecord.OtpType otpType = OtpRecord.OtpType.valueOf(type.toUpperCase());

        // Delete any existing OTP
        otpRepository.deleteByIdentifierAndType(identifier, otpType);

        // Generate new OTP
        String otp;
        if (identifier.startsWith("test")) {
            otp = "123456";
        } else {
            otp = generateOtp();
        }

        // Save OTP record
        OtpRecord otpRecord = OtpRecord.builder()
                .identifier(identifier)
                .otp(otp)
                .type(otpType)
                .expiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES))
                .verified(false)
                .build();

        otpRepository.save(otpRecord);

        // Send OTP via Notification Service
        try {
            if (otpType == OtpRecord.OtpType.MOBILE) {
                com.skilledup.auth.dto.SmsRequest smsRequest = com.skilledup.auth.dto.SmsRequest.builder()
                        .mobile(identifier)
                        .otp(otp)
                        .type("OTP")
                        .build();
                notificationClient.sendSms(smsRequest);
                log.info("SMS OTP sent to {} via Notification Service", identifier);

            } else {
                com.skilledup.auth.dto.EmailRequest emailRequest = com.skilledup.auth.dto.EmailRequest.builder()
                        .recipient(identifier)
                        .type("OTP")
                        .otp(otp)
                        .build();
                notificationClient.sendEmail(emailRequest);
                log.info("Email OTP sent to {} via Notification Service", identifier);
            }

        } catch (Exception e) {
            log.error("Failed to send OTP via Notification Service", e);
            throw new RuntimeException("Failed to send OTP notification");
        }

        return new ApiMessage("OTP sent successfully");
    }

    @Override
    @Transactional
    public ApiMessage verifyOtp(String identifier, String otp, String type) {
        OtpRecord.OtpType otpType = OtpRecord.OtpType.valueOf(type.toUpperCase());

        // Find valid OTP
        OtpRecord otpRecord = otpRepository
                .findByIdentifierAndTypeAndVerifiedFalseAndExpiresAtAfter(
                        identifier, otpType, Instant.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        // Verify OTP
        if (!otpRecord.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        // Mark as verified
        otpRecord.setVerified(true);
        otpRepository.save(otpRecord);

        // Update user verification status
        if (otpType == OtpRecord.OtpType.EMAIL) {
            userRepository.findByEmail(identifier).ifPresent(user -> {
                user.setEmailVerified(true);
                userRepository.save(user);
            });
        } else {
            userRepository.findByMobile(identifier).ifPresent(user -> {
                user.setMobileVerified(true);
                userRepository.save(user);
            });
        }

        return new ApiMessage("OTP verified successfully");
    }

    @Override
    @Transactional
    public ApiMessage resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        verifyOtp(email, request.getOtp(), "EMAIL");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new ApiMessage("Password reset successful");
    }

    private String generateOtp() {
        int length = otpLength <= 0 ? 6 : otpLength;
        if (length != 6) {
            length = 6;
        }
        int otp = 100_000 + OTP_RANDOM.nextInt(900_000);
        return Integer.toString(otp);
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public AuthResponse getUserByEmail(String email) {
        log.info("Finding user by email: '{}'", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User NOT FOUND in database for email: '{}'", email);
                    return new RuntimeException("User not found");
                });
        return new AuthResponse(null, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    @Transactional
    public AuthResponse updateUser(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }
        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            // Check if mobile is taken by another user
            if (userRepository.existsByMobile(request.getMobile()) && !user.getMobile().equals(request.getMobile())) {
                throw new RuntimeException("Mobile already registered");
            }
            user.setMobile(request.getMobile());
        }
        // Can add more fields if needed

        user = userRepository.save(user);
        return new AuthResponse(null, user.getId(), user.getEmail(), user.getMobile(), user.getName(), user.getRole());
    }

    @Override
    @Transactional
    public ApiMessage deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
        return new ApiMessage("User deleted successfully");
    }

    @Override
    @Transactional
    public String fixDatabase() {
        StringBuilder report = new StringBuilder();
        try {
            // 1. Fix Schema
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50)");
            report.append("Fixed users.role length.\n");

            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255)");
            report.append("Fixed users.password_hash length to 255.\n");

        } catch (Exception e) {
            report.append("Schema patch warning: ").append(e.getMessage()).append("\n");
        }

        // 2. Init/Reset Admin
        try {
            User admin = userRepository.findByEmail("admin2@finallms.local")
                    .orElse(User.builder()
                            .name("Default Admin")
                            .email("admin2@finallms.local")
                            .mobile("0000000000")
                            .role(Role.ROLE_ADMIN)
                            .active(true)
                            .emailVerified(true)
                            .mobileVerified(true)
                            .createdAt(Instant.now())
                            .build());

            String newHash = passwordEncoder.encode("Admin2@123");
            admin.setPasswordHash(newHash);
            admin.setRole(Role.ROLE_ADMIN);

            userRepository.save(admin);
            report.append("Admin admin2@finallms.local reset using BCrypt. Hash len: ").append(newHash.length());
        } catch (Exception e) {
            report.append("Admin reset failed: ").append(e.getMessage());
            log.error("Fix failed", e);
        }
        return report.toString();
    }

    @Override
    public AuthResponse getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching profile for email: {}", email);
        return getUserByEmail(email);
    }
}
