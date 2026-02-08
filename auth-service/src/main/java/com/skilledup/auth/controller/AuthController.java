package com.skilledup.auth.controller;

import com.skilledup.auth.dto.ApiMessage;
import com.skilledup.auth.dto.AuthResponse;
import com.skilledup.auth.dto.LoginRequest;
import com.skilledup.auth.dto.RegisterRequest;
import com.skilledup.auth.dto.ResetPasswordRequest;
import com.skilledup.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/fix-db")
    public ResponseEntity<String> fixDb() {
        return ResponseEntity.ok(authService.fixDatabase());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login/otp")
    public ResponseEntity<AuthResponse> loginWithOtp(
            @RequestParam String mobile,
            @RequestParam String otp) {
        return ResponseEntity.ok(authService.loginWithOtp(mobile, otp));
    }

    @PostMapping("/login/otp2")
    public ResponseEntity<AuthResponse> loginWithOtpAny(
            @RequestParam String identifier,
            @RequestParam String otp,
            @RequestParam String type) {
        return ResponseEntity.ok(authService.loginWithOtp(identifier, otp, type));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiMessage> sendOtp(
            @RequestParam String identifier,
            @RequestParam String type) {
        return ResponseEntity.ok(authService.sendOtp(identifier, type));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiMessage> verifyOtp(
            @RequestParam String identifier,
            @RequestParam String otp,
            @RequestParam String type) {
        return ResponseEntity.ok(authService.verifyOtp(identifier, otp, type));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Auth Service is running"));
    }

    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile() {
        return ResponseEntity.ok(authService.getProfile());
    }

    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessage> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<AuthResponse> updateUser(@PathVariable Long id, @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<ApiMessage> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.deleteUser(id));
    }
}
