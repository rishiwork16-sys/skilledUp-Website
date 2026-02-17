package com.skilledup.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLookupResponse {
    private Long id;
    private String email;
    private String mobile;
    private String name;
    private boolean hasPassword;
}

