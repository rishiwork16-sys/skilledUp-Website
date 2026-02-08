package com.skilledup.student.repository;

import com.skilledup.student.model.InternshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipTypeRepository extends JpaRepository<InternshipType, Long> {

    List<InternshipType> findByActiveTrue();
}
