package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.ConsultaRepository;
import com.ramirez.mediturnosback.repository.EspecialidadRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.ProfesionalInstitucionRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import com.ramirez.mediturnosback.repository.TurnoAdjuntoRepository;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TurnoService {

    private static final List<LocalTime> HORARIOS_BASE = List.of(
            LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30),
            LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(15, 0), LocalTime.of(15, 30),
            LocalTime.of(16, 0), LocalTime.of(16, 30), LocalTime.of(17, 0), LocalTime.of(17, 30)
    );
    private static final int DURACION_MINUTOS = 30;

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultaRepository consultaRepository;
    private final TurnoAdjuntoRepository turnoAdjuntoRepository;
    private final VerificationDispatchService verificationDispatchService;

    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        ProfesionalRepository profesionalRepository,
                        ProfesionalInstitucionRepository profesionalInstitucionRepository,
                        EspecialidadRepository especialidadRepository,
                        ConsultaRepository consultaRepository,
                        TurnoAdjuntoRepository turnoAdjuntoRepository,
                        VerificationDispatchService verificationDispatchService) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.consultaRepository = consultaRepository;
        this.turnoAdjuntoRepository = turnoAdjuntoRepository;
        this.verificationDispatchService = verificationDispatchService;
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> listarTodos() { return turnoRepository.findAll().stream().map(this::mapTurno).toList(); }
    
    @Transactional(readOnly = true)
    public TurnoResponse obtenerPorId(Long id) { return mapTurno(obtenerEntidadPorId(id)); }
    
    @Transactional(readOnly = true)
    public List<TurnoResponse> listarPorPaciente(Long pacienteId) { return turnoRepository.findByPacienteIdOrderByFechaHoraInicioDesc(pacienteId).stream().map(this::mapTurno).toList(); }
    
    @Transactional(readOnly = true)
    public List<TurnoResponse> listarHistoriaClinica(Long usuarioId) { return turnoRepository.findByPacienteUsuario_IdAndEstadoOrderByFechaHoraInicioDesc(usuarioId, EstadoTurno.ATENDIDO).stream().map(this::mapTurno).toList(); }

    public List<DisponibilidadSlotResponse> listarDisponibilidad(Long profesionalInstitucionId) {
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(profesionalInstitucionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada con id: " + profesionalInstitucionId));
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(20).atTime(LocalTime.MAX);
        Set<LocalDateTime> ocupados = new HashSet<>();
        turnoRepository.findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(pi.getId(), from, to)
                .stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO && t.getEstado() != EstadoTurno.AUSENTE)
                .forEach(t -> ocupados.add(t.getFechaHoraInicio()));

        List<DisponibilidadSlotResponse> slots = new ArrayList<>();
        for (int dias = 1; dias <= 20; dias++) {
            LocalDate fecha = LocalDate.now().plusDays(dias);
            if (fecha.getDayOfWeek().getValue() >= 6) continue;
            for (LocalTime horario : HORARIOS_BASE) {
                LocalDateTime fechaHora = fecha.atTime(horario);
                if (!ocupados.contains(fechaHora)) {
                    slots.add(new DisponibilidadSlotResponse(fecha.toString(), horario.toString(), fechaHora.toString()));
                }
            }
        }
        return slots;
    }

    public List<TurnoResponse> agendaProfesional(Long usuarioId, LocalDate fecha) {
        LocalDate base = fecha != null ? fecha : LocalDate.now();
        LocalDateTime from = base.atStartOfDay();
        LocalDateTime to = base.atTime(LocalTime.MAX);
        return turnoRepository.findAgendaProfesional(usuarioId, from, to).stream().map(this::mapTurno).toList();
    }

    public TurnoResponse proximoTurnoProfesional(Long usuarioId) {
        return turnoRepository.findFirstByProfesionalUsuario_IdAndFechaHoraInicioAfterAndEstadoInOrderByFechaHoraInicioAsc(
                        usuarioId, LocalDateTime.now(), List.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO, EstadoTurno.REPROGRAMADO))
                .map(this::mapTurno)
                .orElse(null);
    }

    public List<TurnoResponse> historiaPorDni(String dni) {
        return turnoRepository.findHistoriaPorDni(dni).stream().map(this::mapTurno).toList();
    }

    @Transactional
    public TurnoResponse solicitar(TurnoSolicitudRequest request) {
        Paciente paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + request.getPacienteId()));
        Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + request.getProfesionalId()));
        ProfesionalInstitucion pi = resolverProfesionalInstitucion(profesional.getId(), request.getProfesionalInstitucionId());
        Especialidad especialidad = resolverEspecialidad(profesional, request.getEspecialidadId());
        validarDisponibilidad(pi.getId(), request.getFechaHora(), null);

        Turno turno = new Turno();
        turno.setFechaHoraInicio(request.getFechaHora());
        turno.setFechaHoraFin(request.getFechaHora().plusMinutes(DURACION_MINUTOS));
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setObservacionesPaciente(normalizar(request.getObservaciones()));
        turno.setPaciente(paciente);
        turno.setProfesional(profesional);
        turno.setProfesionalInstitucion(pi);
        turno.setEspecialidad(especialidad);
        Turno guardado = turnoRepository.save(turno);

        if (request.getDocumentacionBase64() != null && !request.getDocumentacionBase64().isBlank()) {
            TurnoAdjunto adjunto = new TurnoAdjunto();
            adjunto.setTurno(guardado);
            adjunto.setNombreArchivo(defaultIfBlank(request.getDocumentacionNombreArchivo(), "documentacion.jpg"));
            adjunto.setMimeType(defaultIfBlank(request.getDocumentacionMimeType(), "image/jpeg"));
            adjunto.setContenidoBase64(request.getDocumentacionBase64());
            turnoAdjuntoRepository.save(adjunto);
            guardado.getAdjuntos().add(adjunto);
        }

        TurnoResponse response = mapTurno(guardado);
        String emailPaciente = guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null
                ? guardado.getPaciente().getUsuario().getEmail()
                : null;
        verificationDispatchService.enviarConfirmacionTurno(response, emailPaciente);
        return response;
    }

    @Transactional
    public TurnoResponse reprogramar(Long turnoId, TurnoReprogramacionRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        if (turno.getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Solo se pueden reprogramar turnos próximos");
        }
        Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + request.getProfesionalId()));
        ProfesionalInstitucion pi = resolverProfesionalInstitucion(profesional.getId(), request.getProfesionalInstitucionId());
        Especialidad especialidad = resolverEspecialidad(profesional, request.getEspecialidadId() != null ? request.getEspecialidadId() : turno.getEspecialidad().getId());

        validarDisponibilidad(pi.getId(), request.getFechaHora(), turnoId);
        turno.setFechaHoraInicio(request.getFechaHora());
        turno.setFechaHoraFin(request.getFechaHora().plusMinutes(DURACION_MINUTOS));
        turno.setProfesional(profesional);
        turno.setProfesionalInstitucion(pi);
        turno.setEspecialidad(especialidad);
        turno.setEstado(EstadoTurno.REPROGRAMADO);
        return mapTurno(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse actualizarEstado(Long turnoId, TurnoEstadoUpdateRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        turno.setEstado(request.getEstado());
        return mapTurno(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse cargarDetalleConsulta(Long turnoId, DetalleConsultaRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        Consulta consulta = turno.getConsulta();
        if (consulta == null) {
            consulta = new Consulta();
            consulta.setTurno(turno);
        }
        consulta.setFechaAtencion(LocalDateTime.now());
        consulta.setMotivoConsulta(normalizar(request.getMotivoConsulta()));
        consulta.setEnfermedadActual(normalizar(request.getEnfermedadActual()));
        consulta.setAntecedenteEnfermedadActual(normalizar(request.getAntecedenteEnfermedadActual()));
        consulta.setAntecedentesPersonales(normalizar(request.getAntecedentesPersonales()));
        consulta.setAntecedentesFamiliares(normalizar(request.getAntecedentesFamiliares()));
        consulta.setMedicacionActual(normalizar(request.getMedicacionActual()));
        consulta.setAlergias(normalizar(request.getAlergias()));
        consulta.setHabitos(normalizar(request.getHabitos()));
        consulta.setHallazgosExamenFisico(normalizar(request.getHallazgosExamenFisico()));
        consulta.setConducta(normalizar(request.getConducta()));
        consultaRepository.save(consulta);
        turno.setConsulta(consulta);
        turno.setEstado(EstadoTurno.ATENDIDO);
        return mapTurno(turnoRepository.save(turno));
    }

    @Transactional
    public void eliminar(Long id) { turnoRepository.delete(obtenerEntidadPorId(id)); }

    private Turno obtenerEntidadPorId(Long id) {
        return turnoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + id));
    }

    private void validarDisponibilidad(Long profesionalInstitucionId, LocalDateTime fechaHora, Long turnoIdIgnorado) {
        boolean conflicto = turnoRepository.findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(profesionalInstitucionId, fechaHora.minusMinutes(1), fechaHora.plusMinutes(1))
                .stream()
                .filter(turno -> turno.getEstado() != EstadoTurno.CANCELADO && turno.getEstado() != EstadoTurno.AUSENTE)
                .filter(turno -> turnoIdIgnorado == null || !turno.getId().equals(turnoIdIgnorado))
                .anyMatch(turno -> turno.getFechaHoraInicio().equals(fechaHora));
        if (conflicto) throw new IllegalArgumentException("Ese horario ya no está disponible");
    }

    private ProfesionalInstitucion resolverProfesionalInstitucion(Long profesionalId, Long profesionalInstitucionId) {
        if (profesionalInstitucionId != null) {
            ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(profesionalInstitucionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada con id: " + profesionalInstitucionId));
            if (!pi.getProfesional().getId().equals(profesionalId)) {
                throw new IllegalArgumentException("La sede seleccionada no pertenece al profesional elegido");
            }
            return pi;
        }
        return profesionalInstitucionRepository.findByProfesionalIdAndActivoTrue(profesionalId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El profesional no tiene sedes activas configuradas"));
    }

    private Especialidad resolverEspecialidad(Profesional profesional, Long especialidadId) {
        if (especialidadId != null) {
            Especialidad especialidad = especialidadRepository.findById(especialidadId)
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + especialidadId));
            boolean pertenece = profesional.getEspecialidades().stream().anyMatch(e -> e.getId().equals(especialidad.getId()));
            if (!pertenece) throw new IllegalArgumentException("La especialidad elegida no pertenece al profesional");
            return especialidad;
        }
        return profesional.getEspecialidades().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("El profesional no tiene especialidades configuradas"));
    }

    private TurnoResponse mapTurno(Turno turno) {
        TurnoAdjunto adjunto = turno.getAdjuntos() != null && !turno.getAdjuntos().isEmpty() ? turno.getAdjuntos().get(0) : null;
        Consulta consulta = turno.getConsulta();
        return new TurnoResponse(
                turno.getId(),
                turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(),
                turno.getEstado(),
                turno.getObservacionesPaciente(),
                turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getId() : null,
                turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getNombre() : null,
                turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getDireccion() : null,
                turno.getPaciente() != null ? turno.getPaciente().getId() : null,
                turno.getPaciente() != null ? turno.getPaciente().getNombre() : null,
                turno.getPaciente() != null ? turno.getPaciente().getApellido() : null,
                turno.getPaciente() != null ? turno.getPaciente().getDni() : null,
                turno.getProfesional() != null ? turno.getProfesional().getId() : null,
                turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getId() : null,
                turno.getProfesional() != null ? turno.getProfesional().getNombre() : null,
                turno.getProfesional() != null ? turno.getProfesional().getApellido() : null,
                turno.getEspecialidad() != null ? turno.getEspecialidad().getId() : null,
                turno.getEspecialidad() != null ? turno.getEspecialidad().getNombre() : null,
                turno.getProfesionalInstitucion() != null && turno.getProfesionalInstitucion().getTelefonoEnSede() != null ? turno.getProfesionalInstitucion().getTelefonoEnSede() : turno.getProfesional() != null ? turno.getProfesional().getTelefono() : null,
                adjunto != null ? adjunto.getNombreArchivo() : null,
                adjunto != null ? adjunto.getMimeType() : null,
                adjunto != null ? adjunto.getContenidoBase64() : null,
                consulta != null ? consulta.getMotivoConsulta() : null,
                consulta != null ? consulta.getEnfermedadActual() : null,
                consulta != null ? consulta.getAntecedenteEnfermedadActual() : null,
                consulta != null ? consulta.getAntecedentesPersonales() : null,
                consulta != null ? consulta.getAntecedentesFamiliares() : null,
                consulta != null ? consulta.getMedicacionActual() : null,
                consulta != null ? consulta.getAlergias() : null,
                consulta != null ? consulta.getHabitos() : null,
                consulta != null ? consulta.getHallazgosExamenFisico() : null,
                consulta != null ? consulta.getConducta() : null
        );
    }

    private String defaultIfBlank(String value, String defaultValue) { return value == null || value.isBlank() ? defaultValue : value; }
    private String normalizar(String value) { if (value == null) return null; String trimmed = value.trim(); return trimmed.isEmpty() ? null : trimmed; }
}
