package com.skilledup.task.repository;

import com.skilledup.task.model.StudentTask;
import com.skilledup.task.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTaskRepository extends JpaRepository<StudentTask, Long> {

    List<StudentTask> findByStudentId(Long studentId);

    Optional<StudentTask> findByStudentIdAndTask(Long studentId, Task task);

    boolean existsByStudentIdAndTask(Long studentId, Task task);
    
    // For pending submissions
    List<StudentTask> findByStatus(StudentTask.TaskStatus status);
}
