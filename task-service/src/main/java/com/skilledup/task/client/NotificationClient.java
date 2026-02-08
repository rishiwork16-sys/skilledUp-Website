package com.skilledup.task.client;

import com.skilledup.task.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${application.config.notification-service-url}")
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    void sendEmail(@RequestBody EmailRequest request);
}
