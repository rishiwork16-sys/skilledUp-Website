package com.skilledup.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipConfigurationDTO {
    private Boolean autoStartBatches;
    private String startDay;
    private String frequency;
    private String offerLetterSubject;
    private String offerLetterBody;
    private String offerLetterTemplateKey;
}
