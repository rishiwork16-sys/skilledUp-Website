package com.skilledup.student.repository;

import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipEnrollmentRepository extends JpaRepository<InternshipEnrollment, Long> {

        List<InternshipEnrollment> findByStudent(Student student);

        List<InternshipEnrollment> findByStatus(InternshipEnrollment.EnrollmentStatus status);

        List<InternshipEnrollment> findByInternshipCategory_TitleAndStatus(String title,
                        InternshipEnrollment.EnrollmentStatus status);

        java.util.Optional<InternshipEnrollment> findByStudentAndInternshipCategoryAndStatus(
                        Student student,
                        com.skilledup.student.model.InternshipCategory internshipCategory,
                        InternshipEnrollment.EnrollmentStatus status);

        List<InternshipEnrollment> findByEndDate(java.time.LocalDate endDate);

        void deleteByInternshipCategory(com.skilledup.student.model.InternshipCategory internshipCategory);
}
