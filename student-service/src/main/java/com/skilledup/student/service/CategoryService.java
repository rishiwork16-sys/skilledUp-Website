package com.skilledup.student.service;

import com.skilledup.student.dto.CreateCategoryRequest;
import com.skilledup.student.model.InternshipCategory;
import com.skilledup.student.repository.InternshipCategoryRepository;
import com.skilledup.student.repository.InternshipEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final InternshipCategoryRepository categoryRepository;
    private final S3Service s3Service;
    private final InternshipEnrollmentRepository enrollmentRepository;

    @Transactional
    public InternshipCategory createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByTitle(request.getTitle())) {
            throw new RuntimeException("Category with this title already exists");
        }
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Category with this slug already exists");
        }

        InternshipCategory category = InternshipCategory.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationWeeks(request.getDurationWeeks())
                .slug(request.getSlug())
                .coverImage(request.getCoverImage())

                .tagline(request.getTagline())
                .level(request.getLevel())
                .skills(request.getSkills())
                .tools(request.getTools())
                .maxSeats(request.getMaxSeats())
                .autoStart(request.getAutoStart() != null ? request.getAutoStart() : false)
                .allowReEnrollment(request.getAllowReEnrollment() != null ? request.getAllowReEnrollment() : false)
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .autoCertificate(request.getAutoCertificate() != null ? request.getAutoCertificate() : true)
                .loiPercentage(request.getLoiPercentage() != null ? request.getLoiPercentage() : 80)
                .internalNotes(request.getInternalNotes())
                .active(true)
                .build();

        InternshipCategory saved = categoryRepository.save(category);
        log.info("Created new category: {}", saved.getTitle());
        return saved;
    }

    public List<InternshipCategory> getAllCategories() {
        List<InternshipCategory> categories = categoryRepository.findAll();
        categories.forEach(this::signCategoryImages);
        return categories;
    }

    public List<InternshipCategory> getActiveCategories() {
        List<InternshipCategory> categories = categoryRepository.findByActiveTrue();
        categories.forEach(this::signCategoryImages);
        return categories;
    }

    private void signCategoryImages(InternshipCategory category) {
        if (category.getCoverImage() != null && !category.getCoverImage().isEmpty()) {
            category.setCoverImage(s3Service.generatePresignedUrl(category.getCoverImage()));
        }
    }

    @Transactional
    public InternshipCategory updateCategory(Long id, CreateCategoryRequest request) {
        InternshipCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());
        category.setDurationWeeks(request.getDurationWeeks());
        category.setSlug(request.getSlug());
        category.setCoverImage(request.getCoverImage());
        category.setTagline(request.getTagline());
        category.setLevel(request.getLevel());
        category.setSkills(request.getSkills());
        category.setTools(request.getTools());
        category.setMaxSeats(request.getMaxSeats());
        if (request.getAutoStart() != null)
            category.setAutoStart(request.getAutoStart());
        if (request.getAllowReEnrollment() != null)
            category.setAllowReEnrollment(request.getAllowReEnrollment());
        if (request.getPriority() != null)
            category.setPriority(request.getPriority());
        if (request.getAutoCertificate() != null)
            category.setAutoCertificate(request.getAutoCertificate());
        if (request.getLoiPercentage() != null)
            category.setLoiPercentage(request.getLoiPercentage());
        category.setInternalNotes(request.getInternalNotes());

        InternshipCategory updated = categoryRepository.save(category);
        log.info("Updated category: {}", updated.getTitle());
        return updated;
    }

    @Transactional
    public void deleteCategory(Long id) {
        InternshipCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Hard Delete Logic: Delete all associated enrollments first
        enrollmentRepository.deleteByInternshipCategory(category);

        // Then delete the category itself
        categoryRepository.delete(category);

        log.info("Hard deleted category with id: {} and all associated enrollments", id);
    }
}
