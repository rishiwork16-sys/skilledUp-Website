package com.skilledup.student.repository;

import com.skilledup.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

        Optional<Student> findByEmail(String email);

        Optional<Student> findByUserId(Long userId);

        Optional<Student> findByPhone(String phone);

        boolean existsByEmail(String email);

        // Admin management queries
        List<Student> findByStatus(Student.StudentStatus status);

        List<Student> findByCity(String city);

        List<Student> findByBlocked(boolean blocked);

        long countByStatus(Student.StudentStatus status);

        @Query("SELECT s FROM Student s WHERE " +
                        "(:status IS NULL OR s.status = :status) AND " +
                        "(:city IS NULL OR s.city = :city) AND " +
                        "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<Student> findAllWithFilters(@Param("status") Student.StudentStatus status,
                        @Param("city") String city,
                        @Param("search") String search);
}
