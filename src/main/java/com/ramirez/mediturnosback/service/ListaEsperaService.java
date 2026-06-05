package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.ListaEsperaRequest;
import com.ramirez.mediturnosback.dto.ListaEsperaResponse;
import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListaEsperaService {
    private final ListaEsperaRepository listaEsperaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final VerificationDispatchService verificationDispatchService;
    private final AuditService auditService;

    public ListaEsperaService(ListaEsperaRepository listaEsperaRepository,
                              PacienteRepository pacienteRepository,
                              ProfesionalInstitucionRepository profesionalInstitucionRepository,
                              EspecialidadRepository especialidadRepository,
                              VerificationDispatchService verificationDispatchService,
                              AuditService auditService) {
        this.listaEsperaRepository = listaEsperaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.verificationDispatchService = verificationDispatchService;
        this.auditService = auditService;
    }

    @Transactional
    public ListaEsperaResponse crear(ListaEsperaRequest request) {
        Paciente paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(request.getProfesionalInstitucionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada"));
        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));
        ListaEsperaEntry entry = new ListaEsperaEntry();
        entry.setPaciente(paciente);
        entry.setProfesionalInstitucion(pi);
        entry.setEspecialidad(especialidad);
        entry.setFechaPreferidaDesde(request.getFechaPreferidaDesde());
        entry.setFechaPreferidaHasta(request.getFechaPreferidaHasta());
        entry.setObservaciones(normalizar(request.getObservaciones()));
        ListaEsperaEntry guardado = listaEsperaRepository.save(entry);
        auditService.registrar("LISTA_ESPERA_ALTA", "lista_espera", guardado.getId(), paciente.getUsuario().getEmail(), "Paciente agregado a lista de espera");
        return map(guardado);
    }

    public List<ListaEsperaResponse> listarPorPaciente(Long pacienteId) {
        return listaEsperaRepository.findByPacienteIdOrderByCreadoEnDesc(pacienteId).stream().map(this::map).toList();
    }

    public List<ListaEsperaResponse> listarPendientes() {
        return listaEsperaRepository.findByEstadoOrderByCreadoEnAsc(EstadoListaEspera.PENDIENTE).stream().map(this::map).toList();
    }

    @Transactional
    public void notificarPrimerPendienteCompatible(Turno turno, TurnoResponse turnoResponse) {
        if (turno.getProfesionalInstitucion() == null || turno.getEspecialidad() == null) return;
        listaEsperaRepository.findFirstByProfesionalInstitucionIdAndEspecialidadIdAndEstadoOrderByCreadoEnAsc(
                turno.getProfesionalInstitucion().getId(), turno.getEspecialidad().getId(), EstadoListaEspera.PENDIENTE)
                .ifPresent(entry -> {
                    String email = entry.getPaciente().getUsuario() != null ? entry.getPaciente().getUsuario().getEmail() : null;
                    boolean enviado = verificationDispatchService.enviarAvisoListaEspera(email, entry.getPaciente().getNombre(), turnoResponse);
                    if (enviado) {
                        entry.setEstado(EstadoListaEspera.NOTIFICADO);
                        entry.setNotificadoEn(LocalDateTime.now());
                        listaEsperaRepository.save(entry);
                        auditService.registrar("LISTA_ESPERA_NOTIFICADA", "lista_espera", entry.getId(), "sistema", "Se notificó un turno liberado");
                    }
                });
    }

    private ListaEsperaResponse map(ListaEsperaEntry e) {
        String pacienteNombre = e.getPaciente().getNombre() + " " + e.getPaciente().getApellido();
        return new ListaEsperaResponse(e.getId(), e.getPaciente().getId(), pacienteNombre, e.getProfesionalInstitucion().getId(), e.getEspecialidad().getId(), e.getEspecialidad().getNombre(), e.getFechaPreferidaDesde(), e.getFechaPreferidaHasta(), e.getObservaciones(), e.getEstado().name(), e.getCreadoEn(), e.getNotificadoEn());
    }

    private String normalizar(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
