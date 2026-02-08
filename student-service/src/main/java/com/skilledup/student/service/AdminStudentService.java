package com.skilledup.student.service;

import com.skilledup.student.client.TaskClient;
import com.skilledup.student.dto.ChangeDomainRequest;
import com.skilledup.student.dto.StudentProfileDTO;
import com.skilledup.student.dto.StudentStatsDTO;
import com.skilledup.student.model.InternshipEnrollment;
import com.skilledup.student.model.Student;
import com.skilledup.student.repository.InternshipEnrollmentRepository;
import com.skilledup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStudentService {

    private final StudentRepository studentRepository;
    private final InternshipEnrollmentRepository enrollmentRepository;
    private final TaskClient taskClient;

    public List<Student> getAllStudents(Student.StudentStatus status, String city, String search) {
        if (status == null && city == null && search == null) {
            return studentRepository.findAll();
        }
        return studentRepository.findAllWithFilters(status, city, search);
    }

    private List<Object> getSubmissions(Long studentId) {
        try {
            List<Object> submissions = taskClient.getMySubmissions(studentId);
            log.info("Fetched {} submissions for student {}", submissions != null ? submissions.size() : "null",
                    studentId);
            if (submissions != null && !submissions.isEmpty()) {
                log.info("First submission: {}", submissions.get(0));
            }
            return submissions;
        } catch (Exception e) {
            log.error("Error fetching submissions for student {}", studentId, e);
            // Return dummy data for testing if real fetch fails
            // return List.of(java.util.Map.of(
            // "id", 999,
            // "studentId", studentId,
            // "status", "PENDING",
            // "score", 0,
            // "submittedAt", java.time.Instant.now().toString(),
            // "task", java.util.Map.of("title", "Dummy Debug Task")
            // ));
            return List.of();
        }
    }

    public StudentProfileDTO getStudentProfile(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<InternshipEnrollment> enrollment = enrollmentRepository.findByStudent(student);

        return StudentProfileDTO.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .city(student.getCity())
                .status(student.getStatus())
                .blocked(student.isBlocked())
                .createdAt(student.getCreatedAt())
                .currentDomain(
                        enrollment != null && !enrollment.isEmpty()
                                ? enrollment.get(0).getInternshipCategory().getTitle()
                                : "N/A")
                .completedTasks(calculateCompletedTasks(student.getId()))
                .totalTasks(calculateTotalTasks(student.getId()))
                .progressPercentage(calculateProgress(student.getId()))
                .enrollments(enrollment)
                .submissions(getSubmissions(student.getId()))
                .build();
    }

    private Integer calculateCompletedTasks(Long studentId) {
        try {
            List<?> tasks = (List<?>) taskClient.getMyTasks(studentId);
            if (tasks == null)
                return 0;
            return (int) tasks.stream().filter(t -> {
                if (t instanceof java.util.Map) {
                    Object submitted = ((java.util.Map) t).get("submitted");
                    return Boolean.TRUE.equals(submitted);
                }
                return false;
            }).count();
        } catch (Exception e) {
            log.error("Error fetching tasks for student {}", studentId, e);
            return 0;
        }
    }

    private Integer calculateTotalTasks(Long studentId) {
        try {
            List<?> tasks = (List<?>) taskClient.getMyTasks(studentId);
            return tasks != null ? tasks.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Double calculateProgress(Long studentId) {
        int total = calculateTotalTasks(studentId);
        if (total == 0)
            return 0.0;
        int completed = calculateCompletedTasks(studentId);
        return (double) completed / total * 100;
    }

    @Transactional
    public Student updateStudentStatus(Long studentId, Student.StudentStatus newStatus) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        student.setStatus(newStatus);
        Student updated = studentRepository.save(student);

        log.info("Updated student {} status to {}", studentId, newStatus);
        return updated;
    }

    @Transactional
    public Student toggleBlockStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        student.setBlocked(!student.isBlocked());
        Student updated = studentRepository.save(student);

        log.info("Toggled block status for student {}: {}", studentId, updated.isBlocked());
        return updated;
    }

    @Transactional
    public Student changeStudentDomain(Long studentId, ChangeDomainRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // TODO: Update enrollment domain
        log.info("Changed domain for student {} to {}", studentId, request.getNewDomain());
        return student;
    }

    public StudentStatsDTO getStudentStatistics() {
        long total = studentRepository.count();
        long active = studentRepository.countByStatus(Student.StudentStatus.ACTIVE);
        long completed = studentRepository.countByStatus(Student.StudentStatus.COMPLETED);
        long delayed = studentRepository.countByStatus(Student.StudentStatus.DELAYED);
        long terminated = studentRepository.countByStatus(Student.StudentStatus.TERMINATED);
        long blocked = studentRepository.findByBlocked(true).size();

        return StudentStatsDTO.builder()
                .totalStudents(total)
                .activeStudents(active)
                .completedStudents(completed)
                .delayedStudents(delayed)
                .terminatedStudents(terminated)
                .blockedStudents(blocked)
                .build();
    }
}
