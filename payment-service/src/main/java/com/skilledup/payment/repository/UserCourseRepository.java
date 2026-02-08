package com.skilledup.payment.repository;

import com.skilledup.payment.entity.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);

    List<UserCourse> findByUserId(Long userId);

    boolean existsByUserIdAndCourseIdAndAccessStatus(Long userId, Long courseId, String accessStatus);
}
