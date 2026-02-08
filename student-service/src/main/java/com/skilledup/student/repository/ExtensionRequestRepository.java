package com.skilledup.student.repository;

import com.skilledup.student.model.ExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtensionRequestRepository extends JpaRepository<ExtensionRequest, Long> {

    List<ExtensionRequest> findByStatus(ExtensionRequest.RequestStatus status);

    List<ExtensionRequest> findByStudentId(Long studentId);

    List<ExtensionRequest> findByStudentIdAndStatus(Long studentId, ExtensionRequest.RequestStatus status);
}
