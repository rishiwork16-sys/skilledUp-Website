package com.skilledup.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsRequest {
    private String mobile;
    private String otp;
    private String type; // e.g. "OTP"
}
