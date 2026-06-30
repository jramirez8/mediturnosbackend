package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
public class AgendaService {
    private static final Set<Integer> DURACIONES_VALIDAS = Set.of(10, 15, 20, 30, 45, 60, 90, 120);
    private static final String TABLA_HORARIOS_ATENCION = "horarios_atencion";
    private static final String TABLA_AGENDA_BLOQUEOS = "agenda_bloqueos";
    private static final int DURACION_TURNO_DEFAULT = 30;

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final AgendaBloqueoRepository agendaBloqueoRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public AgendaService(HorarioAtencionRepository horarioAtencionRepository,
                         AgendaBloqueoRepository agendaBloqueoRepository,
                         ProfesionalInstitucionRepository profesionalInstitucionRepository,
                         EspecialidadRepository especialidadRepository,
                         AuditService auditService,
                         CurrentUserService currentUserService) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.agendaBloqueoRepository = agendaBloqueoRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<HorarioAtencionResponse> horarios(Long profesionalInstitucionId) {
        ProfesionalInstitucion pi = obtenerPiConAcceso(profesionalInstitucionId);
        return horarioAtencionRepository.findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(pi.getId()).stream().map(this::mapHorario).toList();
    }

    @Transactional
    public HorarioAtencionResponse crearHorario(HorarioAtencionRequest request) {
        ProfesionalInstitucion pi = obtenerPiConAcceso(request.getProfesionalInstitucionId());
        Especialidad especialidad = obtenerEspecialidadPermitida(pi, request.getEspecialidadId());
        validarHorario(request.getHoraDesde(), request.getHoraHasta(), request.getDuracionTurnoMin());

        HorarioAtencion h = new HorarioAtencion();
        aplicarHorario(h, pi, especialidad, request);
        HorarioAtencion guardado = horarioAtencionRepository.save(h);
        auditService.registrar("AGENDA_HORARIO_ALTA", TABLA_HORARIOS_ATENCION, guardado.getId(), null, "Horario configurado");
        return mapHorario(guardado);
    }

    @Transactional
    public HorarioAtencionResponse actualizarHorario(Long id, HorarioAtencionRequest request) {
        HorarioAtencion h = horarioAtencionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        ProfesionalInstitucion pi = request.getProfesionalInstitucionId() != null
                ? obtenerPiConAcceso(request.getProfesionalInstitucionId())
                : obtenerPiConAcceso(h.getProfesionalInstitucion().getId());
        Especialidad especialidad = obtenerEspecialidadPermitida(pi, request.getEspecialidadId() != null ? request.getEspecialidadId() : h.getEspecialidad().getId());
        validarHorario(request.getHoraDesde() != null ? request.getHoraDesde() : h.getHoraDesde(),
                request.getHoraHasta() != null ? request.getHoraHasta() : h.getHoraHasta(),
                request.getDuracionTurnoMin() != null ? request.getDuracionTurnoMin() : h.getDuracionTurnoMin());

        aplicarHorario(h, pi, especialidad, request);
        HorarioAtencion guardado = horarioAtencionRepository.save(h);
        auditService.registrar("AGENDA_HORARIO_EDICION", TABLA_HORARIOS_ATENCION, guardado.getId(), null, "Horario actualizado");
        return mapHorario(guardado);
    }

    @Transactional
    public void desactivarHorario(Long id) {
        HorarioAtencion h = horarioAtencionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        obtenerPiConAcceso(h.getProfesionalInstitucion().getId());
        h.setActivo(false);
        horarioAtencionRepository.save(h);
        auditService.registrar("AGENDA_HORARIO_BAJA", TABLA_HORARIOS_ATENCION, id, null, "Horario desactivado");
    }

    @Transactional(readOnly = true)
    public List<AgendaBloqueoResponse> bloqueos(Long profesionalInstitucionId) {
        ProfesionalInstitucion pi = obtenerPiConAcceso(profesionalInstitucionId);
        return agendaBloqueoRepository.findByProfesionalInstitucionIdOrderByFechaDesdeAsc(pi.getId()).stream().map(this::mapBloqueo).toList();
    }

    @Transactional
    public AgendaBloqueoResponse crearBloqueo(AgendaBloqueoRequest request) {
        ProfesionalInstitucion pi = obtenerPiConAcceso(request.getProfesionalInstitucionId());
        validarBloqueo(request);
        AgendaBloqueo b = new AgendaBloqueo();
        b.setProfesionalInstitucion(pi);
        b.setFechaDesde(request.getFechaDesde());
        b.setFechaHasta(request.getFechaHasta());
        b.setMotivo(normalizar(request.getMotivo()));
        AgendaBloqueo guardado = agendaBloqueoRepository.save(b);
        auditService.registrar("AGENDA_BLOQUEO_ALTA", TABLA_AGENDA_BLOQUEOS, guardado.getId(), null, "Bloqueo de agenda creado");
        return mapBloqueo(guardado);
    }

    @Transactional
    public AgendaBloqueoResponse actualizarBloqueo(Long id, AgendaBloqueoRequest request) {
        AgendaBloqueo b = agendaBloqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));
        ProfesionalInstitucion pi = request.getProfesionalInstitucionId() != null
                ? obtenerPiConAcceso(request.getProfesionalInstitucionId())
                : obtenerPiConAcceso(b.getProfesionalInstitucion().getId());
        if (request.getFechaDesde() != null) b.setFechaDesde(request.getFechaDesde());
        if (request.getFechaHasta() != null) b.setFechaHasta(request.getFechaHasta());
        validarBloqueo(b.getFechaDesde(), b.getFechaHasta());
        b.setProfesionalInstitucion(pi);
        if (request.getMotivo() != null) b.setMotivo(normalizar(request.getMotivo()));
        AgendaBloqueo guardado = agendaBloqueoRepository.save(b);
        auditService.registrar("AGENDA_BLOQUEO_EDICION", TABLA_AGENDA_BLOQUEOS, guardado.getId(), null, "Bloqueo de agenda actualizado");
        return mapBloqueo(guardado);
    }

    @Transactional
    public void eliminarBloqueo(Long id) {
        AgendaBloqueo b = agendaBloqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));
        obtenerPiConAcceso(b.getProfesionalInstitucion().getId());
        agendaBloqueoRepository.delete(b);
        auditService.registrar("AGENDA_BLOQUEO_BAJA", TABLA_AGENDA_BLOQUEOS, id, null, "Bloqueo de agenda eliminado");
    }

    private void aplicarHorario(HorarioAtencion h, ProfesionalInstitucion pi, Especialidad especialidad, HorarioAtencionRequest request) {
        h.setProfesionalInstitucion(pi);
        h.setEspecialidad(especialidad);
        if (request.getDiaSemana() != null) h.setDiaSemana(normalizarDia(request.getDiaSemana()));
        if (request.getHoraDesde() != null) h.setHoraDesde(request.getHoraDesde());
        if (request.getHoraHasta() != null) h.setHoraHasta(request.getHoraHasta());
        h.setDuracionTurnoMin(resolveDuracionTurno(request, h));
        h.setActivo(resolveActivo(request, h));
    }

    private Integer resolveDuracionTurno(HorarioAtencionRequest request, HorarioAtencion horario) {
        if (request.getDuracionTurnoMin() != null) {
            return request.getDuracionTurnoMin();
        }
        if (horario.getDuracionTurnoMin() != null) {
            return horario.getDuracionTurnoMin();
        }
        return DURACION_TURNO_DEFAULT;
    }

    private Boolean resolveActivo(HorarioAtencionRequest request, HorarioAtencion horario) {
        if (request.getActivo() != null) {
            return request.getActivo();
        }
        if (horario.getActivo() != null) {
            return horario.getActivo();
        }
        return true;
    }

    private ProfesionalInstitucion obtenerPiConAcceso(Long id) {
        if (id == null) throw new IllegalArgumentException("Falta profesionalInstitucionId");
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada"));
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.SECRETARY, RolUsuario.ADMIN);
        if (user.isProfessional()) {
            Long ownerUsuarioId = pi.getProfesional() != null && pi.getProfesional().getUsuario() != null ? pi.getProfesional().getUsuario().getId() : null;
            if (!user.usuarioId().equals(ownerUsuarioId)) {
                throw new AccessDeniedException("No podés modificar la disponibilidad de otro profesional");
            }
        }
        return pi;
    }

    private Especialidad obtenerEspecialidadPermitida(ProfesionalInstitucion pi, Long especialidadId) {
        if (especialidadId == null) {
            return pi.getProfesional().getEspecialidades().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El profesional no tiene especialidades configuradas"));
        }
        Especialidad especialidad = especialidadRepository.findById(especialidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));
        boolean pertenece = pi.getProfesional().getEspecialidades().stream().anyMatch(e -> e.getId().equals(especialidad.getId()));
        if (!pertenece) throw new IllegalArgumentException("La especialidad elegida no pertenece al profesional");
        return especialidad;
    }

    private void validarHorario(LocalTime desde, LocalTime hasta, Integer duracion) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El horario necesita hora desde/hasta válida");
        }
        int minutos = duracion != null ? duracion : DURACION_TURNO_DEFAULT;
        if (!DURACIONES_VALIDAS.contains(minutos)) {
            throw new IllegalArgumentException("Duración inválida. Usá 10, 15, 20, 30, 45, 60, 90 o 120 minutos");
        }
        if (desde.plusMinutes(minutos).isAfter(hasta)) {
            throw new IllegalArgumentException("El rango horario es más chico que la duración del turno");
        }
    }

    private void validarBloqueo(AgendaBloqueoRequest request) {
        validarBloqueo(request.getFechaDesde(), request.getFechaHasta());
    }

    private void validarBloqueo(java.time.LocalDateTime desde, java.time.LocalDateTime hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El bloqueo necesita fecha desde/hasta válida");
        }
    }

    private HorarioAtencionResponse mapHorario(HorarioAtencion h) {
        return new HorarioAtencionResponse(h.getId(), h.getProfesionalInstitucion().getId(), h.getEspecialidad().getId(), h.getEspecialidad().getNombre(), h.getDiaSemana(), h.getHoraDesde(), h.getHoraHasta(), h.getDuracionTurnoMin(), h.getActivo());
    }

    private AgendaBloqueoResponse mapBloqueo(AgendaBloqueo b) {
        return new AgendaBloqueoResponse(b.getId(), b.getProfesionalInstitucion().getId(), b.getFechaDesde(), b.getFechaHasta(), b.getMotivo());
    }

    private String normalizarDia(String dia) {
        if (dia == null || dia.isBlank()) throw new IllegalArgumentException("Falta día de semana");
        String d = dia.trim().toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");
        return switch (d) {
            case "LUN", "LUNES", "MONDAY" -> "LUNES";
            case "MAR", "MARTES", "TUESDAY" -> "MARTES";
            case "MIE", "MIER", "MIERCOLES", "WEDNESDAY" -> "MIERCOLES";
            case "JUE", "JUEVES", "THURSDAY" -> "JUEVES";
            case "VIE", "VIERNES", "FRIDAY" -> "VIERNES";
            case "SAB", "SABADO", "SATURDAY" -> "SABADO";
            case "DOM", "DOMINGO", "SUNDAY" -> "DOMINGO";
            default -> throw new IllegalArgumentException("Día de semana inválido: " + dia);
        };
    }

    private String normalizar(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
