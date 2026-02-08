package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentStatsDTO {
    private long totalStudents;
    private long activeStudents;
    private long completedStudents;
    private long delayedStudents;
    private long terminatedStudents;
    private long blockedStudents;
}
