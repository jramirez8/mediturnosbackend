package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.FeedbackTurnoRequest;
import com.ramirez.mediturnosback.dto.FeedbackTurnoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.EstadoTurno;
import com.ramirez.mediturnosback.model.FeedbackTurno;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Turno;
import com.ramirez.mediturnosback.repository.FeedbackTurnoRepository;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class FeedbackService {
    private final FeedbackTurnoRepository feedbackTurnoRepository;
    private final TurnoRepository turnoRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public FeedbackService(FeedbackTurnoRepository feedbackTurnoRepository, TurnoRepository turnoRepository, AuditService auditService, CurrentUserService currentUserService) {
        this.feedbackTurnoRepository = feedbackTurnoRepository;
        this.turnoRepository = turnoRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public FeedbackTurnoResponse guardar(Long turnoId, FeedbackTurnoRequest request) {
        Turno turno = turnoRepository.findById(turnoId).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        validarPacienteDelTurno(turno);
        if (turno.getEstado() != EstadoTurno.ATENDIDO) throw new IllegalArgumentException("Solo se puede calificar un turno atendido");
        int puntuacion = request.getPuntuacion() == null ? 0 : request.getPuntuacion();
        if (puntuacion < 1 || puntuacion > 5) throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
        FeedbackTurno feedback = feedbackTurnoRepository.findByTurnoId(turnoId).orElseGet(FeedbackTurno::new);
        feedback.setTurno(turno);
        feedback.setPuntuacion(puntuacion);
        feedback.setComentario(request.getComentario() == null ? null : request.getComentario().trim());
        FeedbackTurno guardado = feedbackTurnoRepository.save(feedback);
        auditService.registrar("FEEDBACK_TURNO", "turnos", turnoId, null, "Paciente calificó el turno");
        return map(guardado);
    }

    public FeedbackTurnoResponse obtener(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        validarLectura(turno);
        return feedbackTurnoRepository.findByTurnoId(turnoId).map(this::map).orElse(null);
    }

    public List<FeedbackTurnoResponse> ultimos() {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY, RolUsuario.PROFESSIONAL);
        return feedbackTurnoRepository.findTop50ByOrderByCreadoEnDesc().stream()
                .filter(feedback -> !user.isProfessional()
                        || (feedback.getTurno() != null
                        && feedback.getTurno().getProfesional() != null
                        && Objects.equals(feedback.getTurno().getProfesional().getId(), user.profesionalId())))
                .map(this::map)
                .toList();
    }

    private void validarPacienteDelTurno(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && turno.getPaciente() != null && Objects.equals(turno.getPaciente().getId(), user.pacienteId())) return;
        throw new AccessDeniedException("No podés calificar turnos de otro paciente");
    }

    private void validarLectura(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && turno.getPaciente() != null && Objects.equals(turno.getPaciente().getId(), user.pacienteId())) return;
        if (user.isProfessional() && turno.getProfesional() != null && Objects.equals(turno.getProfesional().getId(), user.profesionalId())) return;
        throw new AccessDeniedException("No tenés acceso a este feedback");
    }

    private FeedbackTurnoResponse map(FeedbackTurno f) {
        Turno turno = f.getTurno();
        String profesionalNombre = turno != null && turno.getProfesional() != null
                ? (turno.getProfesional().getNombre() + " " + turno.getProfesional().getApellido()).trim()
                : null;
        return new FeedbackTurnoResponse(
                f.getId(),
                turno != null ? turno.getId() : null,
                turno != null && turno.getProfesional() != null ? turno.getProfesional().getId() : null,
                profesionalNombre,
                f.getPuntuacion(),
                f.getComentario(),
                f.getCreadoEn()
        );
    }
}
