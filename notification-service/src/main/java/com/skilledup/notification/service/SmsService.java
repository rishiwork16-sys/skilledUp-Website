package com.skilledup.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class SmsService {

    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";

    @org.springframework.beans.factory.annotation.Value("${fast2sms.api.key}")
    private String authKey;

    private static final String DLT_TEMPLATE_ID = "182903";

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean sendOtp(String mobileNumber, String otp) {
        try {
            // Clean and format phone number
            String cleanedNumber = mobileNumber.trim().replaceAll("[^0-9]", ""); // Remove all non-numeric characters

            String formattedNumber;
            if (cleanedNumber.startsWith("91") && cleanedNumber.length() == 12) {
                // Already has 91 prefix with 10 digits
                formattedNumber = cleanedNumber;
            } else if (cleanedNumber.length() == 10) {
                // 10-digit number, add 91 prefix
                formattedNumber = "91" + cleanedNumber;
            } else if (cleanedNumber.length() == 12 && cleanedNumber.startsWith("91")) {
                // Already formatted correctly
                formattedNumber = cleanedNumber;
            } else {
                log.error("❌ Invalid phone number format: {} (cleaned: {})", mobileNumber, cleanedNumber);
                return false;
            }

            // Build Fast2SMS URL with proper parameters
            String url = FAST2SMS_URL
                    + "?authorization=" + authKey
                    + "&route=dlt"
                    + "&sender_id=SKLDUP"
                    + "&message=" + DLT_TEMPLATE_ID
                    + "&variables_values=" + otp
                    + "&numbers=" + formattedNumber
                    + "&flash=0";

            log.info("📱 Sending SMS OTP to {} (formatted: {})", mobileNumber, formattedNumber);

            String response = restTemplate.getForObject(url, String.class);

            log.info("📨 Fast2SMS Response: {}", response);

            if (response != null && response.contains("\"return\":true")) {
                log.info("✅ SMS OTP sent successfully to {} | OTP: {}", formattedNumber, otp);
                return true;
            } else {
                log.error("❌ Failed to send SMS OTP to {} | Response: {}", formattedNumber, response);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Exception sending SMS OTP to {}: {}", mobileNumber, e.getMessage(), e);
            return false;
        }
    }
}
