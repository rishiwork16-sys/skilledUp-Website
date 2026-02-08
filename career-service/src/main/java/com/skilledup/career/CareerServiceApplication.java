package com.skilledup.career;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CareerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerServiceApplication.class, args);
    }
}
