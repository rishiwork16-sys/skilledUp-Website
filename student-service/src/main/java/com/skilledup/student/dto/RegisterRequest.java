package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String mobile;
    private String city;
    private Long internshipTypeId;
    private Integer duration;
    private boolean emailVerified;
    private boolean mobileVerified;
    // Address fields if needed, but keeping simple for now
}
