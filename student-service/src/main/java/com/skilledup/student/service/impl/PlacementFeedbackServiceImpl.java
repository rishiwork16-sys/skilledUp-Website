package com.skilledup.student.service.impl;

import com.skilledup.student.model.PlacementFeedback;
import com.skilledup.student.repository.PlacementFeedbackRepository;
import com.skilledup.student.service.PlacementFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacementFeedbackServiceImpl implements PlacementFeedbackService {

    private final PlacementFeedbackRepository feedbackRepository;

    @Override
    public PlacementFeedback submitFeedback(PlacementFeedback feedback) {
        return feedbackRepository.save(feedback);
    }

    @Override
    public List<PlacementFeedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }
}
