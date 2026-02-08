package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompletionStatsDTO {
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer completionPercent;
    private Boolean meetsMinimumRequirement; // >= 95%
}
