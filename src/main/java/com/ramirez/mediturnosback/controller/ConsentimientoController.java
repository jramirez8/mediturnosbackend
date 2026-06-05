package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.ConsentimientoRequest;
import com.ramirez.mediturnosback.dto.ConsentimientoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/pacientes")
public class ConsentimientoController {
    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;

    public ConsentimientoController(PacienteRepository pacienteRepository, AuditService auditService) {
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
    }

    @PutMapping("/{pacienteId}/consentimiento")
    public ConsentimientoResponse aceptar(@PathVariable Long pacienteId, @RequestBody ConsentimientoRequest request) {
        Paciente p = pacienteRepository.findById(pacienteId).orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        boolean aceptado = Boolean.TRUE.equals(request.getAceptado());
        p.setConsentimientoDatosAceptado(aceptado);
        p.setConsentimientoDatosAceptadoEn(aceptado ? LocalDateTime.now() : null);
        p.setConsentimientoTexto(request.getTexto());
        pacienteRepository.save(p);
        auditService.registrar("CONSENTIMIENTO_DATOS", "pacientes", pacienteId, p.getUsuario().getEmail(), aceptado ? "Aceptó consentimiento" : "Revocó consentimiento");
        return new ConsentimientoResponse(p.getId(), p.getConsentimientoDatosAceptado(), p.getConsentimientoDatosAceptadoEn(), p.getConsentimientoTexto());
    }
}
