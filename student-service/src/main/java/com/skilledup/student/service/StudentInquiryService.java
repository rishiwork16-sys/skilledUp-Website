package com.skilledup.student.service;

import com.skilledup.student.dto.StudentInquiryRequest;
import com.skilledup.student.dto.StudentInquiryUpdateRequest;
import com.skilledup.student.model.StudentInquiry;
import com.skilledup.student.repository.StudentInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentInquiryService {
    private final StudentInquiryRepository repository;

    @Transactional
    public StudentInquiry create(StudentInquiryRequest request) {
        StudentInquiry inquiry = StudentInquiry.builder()
                .fullName(safe(request.getFullName()))
                .email(safe(request.getEmail()))
                .mobileNumber(safe(request.getMobileNumber()))
                .city(safe(request.getCity()))
                .stateName(safe(request.getStateName()))
                .pinCode(safe(request.getPinCode()))
                .backgroundName(safe(request.getBackgroundName()))
                .courseId(request.getCourseId())
                .courseTitle(safe(request.getCourseTitle()))
                .pagePath(safe(request.getPagePath()))
                .build();
        return repository.save(inquiry);
    }

    public List<StudentInquiry> listAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public StudentInquiry update(Long id, StudentInquiryUpdateRequest request) {
        StudentInquiry inquiry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));

        if (request.getStatus() != null) {
            inquiry.setStatus(request.getStatus());
        }
        if (request.getAdminNotes() != null) {
            inquiry.setAdminNotes(request.getAdminNotes());
        }
        return repository.save(inquiry);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

