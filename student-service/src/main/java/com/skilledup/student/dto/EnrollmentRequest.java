package com.skilledup.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequest {

    private Long userId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;
    private String address;
    private String state;
    private String country;
    private String pincode;

    private String city;

    @NotNull(message = "Internship type ID is required")
    private Long internshipTypeId;

    @NotNull(message = "Duration is required")
    private Integer duration; // in months
}
