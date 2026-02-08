package com.skilledup.student.client;

import com.skilledup.student.dto.ApiMessage;
import com.skilledup.student.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    ApiMessage sendEmail(@RequestBody EmailRequest request);
}
