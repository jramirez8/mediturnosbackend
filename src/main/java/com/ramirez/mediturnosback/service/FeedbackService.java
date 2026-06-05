package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.FeedbackTurnoRequest;
import com.ramirez.mediturnosback.dto.FeedbackTurnoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.EstadoTurno;
import com.ramirez.mediturnosback.model.FeedbackTurno;
import com.ramirez.mediturnosback.model.Turno;
import com.ramirez.mediturnosback.repository.FeedbackTurnoRepository;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackTurnoRepository feedbackTurnoRepository;
    private final TurnoRepository turnoRepository;
    private final AuditService auditService;

    public FeedbackService(FeedbackTurnoRepository feedbackTurnoRepository, TurnoRepository turnoRepository, AuditService auditService) {
        this.feedbackTurnoRepository = feedbackTurnoRepository;
        this.turnoRepository = turnoRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FeedbackTurnoResponse guardar(Long turnoId, FeedbackTurnoRequest request) {
        Turno turno = turnoRepository.findById(turnoId).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        if (turno.getEstado() != EstadoTurno.ATENDIDO) throw new IllegalArgumentException("Solo se puede calificar un turno atendido");
        int puntuacion = request.getPuntuacion() == null ? 0 : request.getPuntuacion();
        if (puntuacion < 1 || puntuacion > 5) throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
        FeedbackTurno feedback = feedbackTurnoRepository.findByTurnoId(turnoId).orElseGet(FeedbackTurno::new);
        feedback.setTurno(turno);
        feedback.setPuntuacion(puntuacion);
        feedback.setComentario(request.getComentario() == null ? null : request.getComentario().trim());
        FeedbackTurno guardado = feedbackTurnoRepository.save(feedback);
        auditService.registrar("FEEDBACK_TURNO", "turnos", turnoId, turno.getPaciente().getUsuario().getEmail(), "Paciente calificó el turno");
        return map(guardado);
    }

    public FeedbackTurnoResponse obtener(Long turnoId) {
        return feedbackTurnoRepository.findByTurnoId(turnoId).map(this::map).orElse(null);
    }

    public List<FeedbackTurnoResponse> ultimos() {
        return feedbackTurnoRepository.findTop50ByOrderByCreadoEnDesc().stream().map(this::map).toList();
    }

    private FeedbackTurnoResponse map(FeedbackTurno f) {
        return new FeedbackTurnoResponse(f.getId(), f.getTurno().getId(), f.getPuntuacion(), f.getComentario(), f.getCreadoEn());
    }
}
