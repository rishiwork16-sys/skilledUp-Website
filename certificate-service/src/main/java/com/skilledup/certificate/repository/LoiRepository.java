package com.skilledup.certificate.repository;

import com.skilledup.certificate.model.LoiRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoiRepository extends JpaRepository<LoiRecord, Long> {

    Optional<LoiRecord> findByStudentId(Long studentId);
}
