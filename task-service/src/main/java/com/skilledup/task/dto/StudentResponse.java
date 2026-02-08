package com.skilledup.task.dto;

import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String city;
}
