package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.FeedbackTurnoRequest;
import com.ramirez.mediturnosback.dto.FeedbackTurnoResponse;
import com.ramirez.mediturnosback.service.FeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/{id}/feedback")
    public FeedbackTurnoResponse guardar(@PathVariable Long id, @RequestBody FeedbackTurnoRequest request) {
        return feedbackService.guardar(id, request);
    }

    @GetMapping("/{id}/feedback")
    public FeedbackTurnoResponse obtener(@PathVariable Long id) {
        return feedbackService.obtener(id);
    }

    @GetMapping("/feedback")
    public List<FeedbackTurnoResponse> ultimos() {
        return feedbackService.ultimos();
    }
}
