package com.skilledup.student.service;

import com.skilledup.student.model.PlacementFeedback;
import java.util.List;

public interface PlacementFeedbackService {
    PlacementFeedback submitFeedback(PlacementFeedback feedback);

    List<PlacementFeedback> getAllFeedbacks();
}
