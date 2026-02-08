package com.skilledup.student.repository;

import com.skilledup.student.model.StudentInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInquiryRepository extends JpaRepository<StudentInquiry, Long> {
}

