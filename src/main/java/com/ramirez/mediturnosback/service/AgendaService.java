package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgendaService {
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final AgendaBloqueoRepository agendaBloqueoRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final AuditService auditService;

    public AgendaService(HorarioAtencionRepository horarioAtencionRepository,
                         AgendaBloqueoRepository agendaBloqueoRepository,
                         ProfesionalInstitucionRepository profesionalInstitucionRepository,
                         EspecialidadRepository especialidadRepository,
                         AuditService auditService) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.agendaBloqueoRepository = agendaBloqueoRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.auditService = auditService;
    }

    public List<HorarioAtencionResponse> horarios(Long profesionalInstitucionId) {
        return horarioAtencionRepository.findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(profesionalInstitucionId).stream().map(this::mapHorario).toList();
    }

    @Transactional
    public HorarioAtencionResponse crearHorario(HorarioAtencionRequest request) {
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(request.getProfesionalInstitucionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada"));
        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));
        HorarioAtencion h = new HorarioAtencion();
        h.setProfesionalInstitucion(pi);
        h.setEspecialidad(especialidad);
        h.setDiaSemana(request.getDiaSemana());
        h.setHoraDesde(request.getHoraDesde());
        h.setHoraHasta(request.getHoraHasta());
        h.setDuracionTurnoMin(request.getDuracionTurnoMin() != null ? request.getDuracionTurnoMin() : 30);
        h.setActivo(request.getActivo() != null ? request.getActivo() : true);
        HorarioAtencion guardado = horarioAtencionRepository.save(h);
        auditService.registrar("AGENDA_HORARIO_ALTA", "horarios_atencion", guardado.getId(), "admin", "Horario configurado");
        return mapHorario(guardado);
    }

    @Transactional
    public void desactivarHorario(Long id) {
        HorarioAtencion h = horarioAtencionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        h.setActivo(false);
        horarioAtencionRepository.save(h);
        auditService.registrar("AGENDA_HORARIO_BAJA", "horarios_atencion", id, "admin", "Horario desactivado");
    }

    public List<AgendaBloqueoResponse> bloqueos(Long profesionalInstitucionId) {
        return agendaBloqueoRepository.findByProfesionalInstitucionIdOrderByFechaDesdeAsc(profesionalInstitucionId).stream().map(this::mapBloqueo).toList();
    }

    @Transactional
    public AgendaBloqueoResponse crearBloqueo(AgendaBloqueoRequest request) {
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(request.getProfesionalInstitucionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada"));
        if (request.getFechaDesde() == null || request.getFechaHasta() == null || !request.getFechaHasta().isAfter(request.getFechaDesde())) {
            throw new IllegalArgumentException("El bloqueo necesita fecha desde/hasta válida");
        }
        AgendaBloqueo b = new AgendaBloqueo();
        b.setProfesionalInstitucion(pi);
        b.setFechaDesde(request.getFechaDesde());
        b.setFechaHasta(request.getFechaHasta());
        b.setMotivo(request.getMotivo());
        AgendaBloqueo guardado = agendaBloqueoRepository.save(b);
        auditService.registrar("AGENDA_BLOQUEO_ALTA", "agenda_bloqueos", guardado.getId(), "admin", "Bloqueo de agenda creado");
        return mapBloqueo(guardado);
    }

    @Transactional
    public void eliminarBloqueo(Long id) {
        agendaBloqueoRepository.deleteById(id);
        auditService.registrar("AGENDA_BLOQUEO_BAJA", "agenda_bloqueos", id, "admin", "Bloqueo de agenda eliminado");
    }

    private HorarioAtencionResponse mapHorario(HorarioAtencion h) {
        return new HorarioAtencionResponse(h.getId(), h.getProfesionalInstitucion().getId(), h.getEspecialidad().getId(), h.getEspecialidad().getNombre(), h.getDiaSemana(), h.getHoraDesde(), h.getHoraHasta(), h.getDuracionTurnoMin(), h.getActivo());
    }

    private AgendaBloqueoResponse mapBloqueo(AgendaBloqueo b) {
        return new AgendaBloqueoResponse(b.getId(), b.getProfesionalInstitucion().getId(), b.getFechaDesde(), b.getFechaHasta(), b.getMotivo());
    }
}
