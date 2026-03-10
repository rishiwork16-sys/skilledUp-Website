package com.skilledup.student.repository;

import com.skilledup.student.model.PlacementFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementFeedbackRepository extends JpaRepository<PlacementFeedback, Long> {
}
