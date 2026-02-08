package com.skilledup.student.repository;

import com.skilledup.student.model.InternshipConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternshipConfigurationRepository extends JpaRepository<InternshipConfiguration, Long> {
    // Since we only have one config, we don't need complex queries
}
