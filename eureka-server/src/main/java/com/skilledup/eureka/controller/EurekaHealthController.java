package com.skilledup.eureka.controller;

import com.skilledup.eureka.dto.ApiMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EurekaHealthController {

    @GetMapping("/api/eureka/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Eureka Server is running"));
    }
}
