package com.skilledup.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile is required")
    @Size(min = 10, max = 15, message = "Mobile must be 10-15 digits")
    private String mobile;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private boolean emailVerified = false;
    private boolean mobileVerified = false;
}
