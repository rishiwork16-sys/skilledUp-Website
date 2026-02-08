package com.skilledup.student.repository;

import com.skilledup.student.model.ExitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExitRequestRepository extends JpaRepository<ExitRequest, Long> {

    List<ExitRequest> findByStatus(ExitRequest.RequestStatus status);

    List<ExitRequest> findByStudentId(Long studentId);

    List<ExitRequest> findByStudentIdAndStatus(Long studentId, ExitRequest.RequestStatus status);
}
