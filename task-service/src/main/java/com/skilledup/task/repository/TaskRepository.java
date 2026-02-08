package com.skilledup.task.repository;

import com.skilledup.task.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByDomainAndActiveTrue(String domain);

    Optional<Task> findByDomainAndWeekNoAndActiveTrue(String domain, Integer weekNo);
}
