package com.skilledup.task.repository;

import com.skilledup.task.model.ExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtensionRequestRepository extends JpaRepository<ExtensionRequest, Long> {

    List<ExtensionRequest> findByStudentId(Long studentId);

    List<ExtensionRequest> findByStatus(ExtensionRequest.ExtensionStatus status);

    Optional<ExtensionRequest> findByStudentIdAndTaskId(Long studentId, Long taskId);
}
