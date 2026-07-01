package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.ConsentimientoRequest;
import com.ramirez.mediturnosback.dto.ConsentimientoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.service.AuditService;
import com.ramirez.mediturnosback.util.AppClock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/pacientes")
public class ConsentimientoController {
    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public ConsentimientoController(PacienteRepository pacienteRepository, AuditService auditService, CurrentUserService currentUserService) {
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @PutMapping("/{pacienteId}/consentimiento")
    public ConsentimientoResponse aceptar(@PathVariable Long pacienteId, @RequestBody ConsentimientoRequest request) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isPatient() && !pacienteId.equals(user.pacienteId())) {
            throw new AccessDeniedException("No podés modificar el consentimiento de otro paciente");
        }
        Paciente p = pacienteRepository.findById(pacienteId).orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        boolean aceptado = Boolean.TRUE.equals(request.getAceptado());
        p.setConsentimientoDatosAceptado(aceptado);
        p.setConsentimientoDatosAceptadoEn(aceptado ? LocalDateTime.now(AppClock.APP_ZONE) : null);
        p.setConsentimientoTexto(request.getTexto());
        pacienteRepository.save(p);
        auditService.registrar("CONSENTIMIENTO_DATOS", "pacientes", pacienteId, null, aceptado ? "Aceptó consentimiento" : "Revocó consentimiento");
        return new ConsentimientoResponse(p.getId(), p.getConsentimientoDatosAceptado(), p.getConsentimientoDatosAceptadoEn(), p.getConsentimientoTexto());
    }
}
