package com.skilledup.student.repository;

import com.skilledup.student.model.LORRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LORRequestRepository extends JpaRepository<LORRequest, Long> {

    // Find by student and enrollment
    Optional<LORRequest> findByStudentIdAndEnrollmentId(Long studentId, Long enrollmentId);

    // Find all requests by student
    List<LORRequest> findByStudentId(Long studentId);

    // Find by status
    List<LORRequest> findByRequestStatus(LORRequest.RequestStatus status);

    // Find pending requests (for admin)
    @Query("SELECT lr FROM LORRequest lr WHERE lr.requestStatus = 'PENDING' ORDER BY lr.requestedAt ASC")
    List<LORRequest> findPendingRequests();

    // Find approved but not yet generated (for scheduler)
    @Query("SELECT lr FROM LORRequest lr WHERE lr.requestStatus = 'APPROVED' AND lr.generatedAt IS NULL")
    List<LORRequest> findApprovedNotGenerated();

    // Find requests eligible for auto-generation based on internship end date
    @Query("SELECT lr FROM LORRequest lr " +
            "JOIN InternshipEnrollment ie ON lr.enrollmentId = ie.id " +
            "WHERE lr.requestStatus = 'APPROVED' " +
            "AND lr.generatedAt IS NULL " +
            "AND ie.endDate <= :currentDate")
    List<LORRequest> findEligibleForAutoGeneration(@Param("currentDate") LocalDate currentDate);

    // Check if LOR already exists for enrollment
    boolean existsByStudentIdAndEnrollmentId(Long studentId, Long enrollmentId);
}
