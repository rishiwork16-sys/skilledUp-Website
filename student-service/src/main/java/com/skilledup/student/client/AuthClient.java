package com.skilledup.student.client;

import com.skilledup.student.dto.AuthResponse;
import com.skilledup.student.dto.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/api/auth/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    @org.springframework.web.bind.annotation.GetMapping("/api/auth/user")
    AuthResponse getUserByEmail(@org.springframework.web.bind.annotation.RequestParam("email") String email);

    @org.springframework.web.bind.annotation.PutMapping("/api/auth/user/{id}")
    AuthResponse updateUser(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
            @RequestBody RegisterRequest request);

    @org.springframework.web.bind.annotation.DeleteMapping("/api/auth/user/{id}")
    com.skilledup.student.dto.ApiMessage deleteUser(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
