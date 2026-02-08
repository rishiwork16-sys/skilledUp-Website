package com.skilledup.auth.client;

import com.skilledup.auth.dto.ApiMessage;
import com.skilledup.auth.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    ApiMessage sendEmail(@RequestBody EmailRequest request);

    @PostMapping("/api/notifications/send-sms")
    ApiMessage sendSms(@RequestBody com.skilledup.auth.dto.SmsRequest request);
}
