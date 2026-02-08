package com.skilledup.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_schedule", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "task_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private LocalDate unlockDate; // Monday when task unlocks

    @Column(nullable = false)
    private LocalDateTime deadline; // Sunday 11:59 PM

    @Column(nullable = false)
    private boolean isUnlocked = false;

    @Column(nullable = false)
    private boolean isSubmitted = false;

    @Column(nullable = false)
    private boolean isDelayed = false;

    private LocalDateTime lastReminderSentAt;
}
