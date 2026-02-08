package com.skilledup.task.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "student-service", url = "${application.config.student-service-url:http://localhost:8082}")
public interface StudentClient {

    @GetMapping("/api/students/search/active-by-domain")
    List<Long> getActiveStudentsByDomain(@RequestParam("domain") String domain);

    @GetMapping("/api/students/{id}")
    com.skilledup.task.dto.StudentResponse getStudentById(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
