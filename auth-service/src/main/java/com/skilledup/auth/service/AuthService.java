package com.skilledup.auth.service;

import com.skilledup.auth.dto.ApiMessage;
import com.skilledup.auth.dto.AuthResponse;
import com.skilledup.auth.dto.LoginRequest;
import com.skilledup.auth.dto.RegisterRequest;
import com.skilledup.auth.dto.ResetPasswordRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithOtp(String mobile, String otp);

    AuthResponse loginWithOtp(String identifier, String otp, String type); // EMAIL or MOBILE

    ApiMessage sendOtp(String identifier, String type); // type: EMAIL or MOBILE

    ApiMessage verifyOtp(String identifier, String otp, String type);

    AuthResponse getUserByEmail(String email);

    AuthResponse updateUser(Long id, RegisterRequest request);

    ApiMessage deleteUser(Long id);

    String fixDatabase();

    AuthResponse getProfile();

    ApiMessage resetPassword(ResetPasswordRequest request);
}
