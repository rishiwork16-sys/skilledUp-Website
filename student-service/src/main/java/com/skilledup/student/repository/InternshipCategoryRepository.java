package com.skilledup.student.repository;

import com.skilledup.student.model.InternshipCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipCategoryRepository extends JpaRepository<InternshipCategory, Long> {

    List<InternshipCategory> findByActiveTrue();

    Optional<InternshipCategory> findByTitle(String title);

    boolean existsByTitle(String title);

    boolean existsBySlug(String slug);
}
