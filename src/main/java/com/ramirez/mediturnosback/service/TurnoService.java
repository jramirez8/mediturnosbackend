package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.util.AppClock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class TurnoService {

    private static final int DURACION_MINUTOS = 30;
    private static final String TABLA_TURNOS = "turnos";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultaRepository consultaRepository;
    private final TurnoAdjuntoRepository turnoAdjuntoRepository;
    private final VerificationDispatchService verificationDispatchService;
    private final MediaFileService mediaFileService;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final AgendaBloqueoRepository agendaBloqueoRepository;
    private final ListaEsperaService listaEsperaService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final AppClock appClock;

    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        ProfesionalRepository profesionalRepository,
                        ProfesionalInstitucionRepository profesionalInstitucionRepository,
                        EspecialidadRepository especialidadRepository,
                        ConsultaRepository consultaRepository,
                        TurnoAdjuntoRepository turnoAdjuntoRepository,
                        VerificationDispatchService verificationDispatchService,
                        MediaFileService mediaFileService,
                        HorarioAtencionRepository horarioAtencionRepository,
                        AgendaBloqueoRepository agendaBloqueoRepository,
                        ListaEsperaService listaEsperaService,
                        AuditService auditService,
                        CurrentUserService currentUserService,
                        AppClock appClock) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.consultaRepository = consultaRepository;
        this.turnoAdjuntoRepository = turnoAdjuntoRepository;
        this.verificationDispatchService = verificationDispatchService;
        this.mediaFileService = mediaFileService;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.agendaBloqueoRepository = agendaBloqueoRepository;
        this.listaEsperaService = listaEsperaService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.appClock = appClock;
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> listarTodos() {
        currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY);
        return turnoRepository.findAll().stream().map(this::mapTurno).toList();
    }

    @Transactional(readOnly = true)
    public TurnoResponse obtenerPorId(Long id) {
        Turno turno = obtenerEntidadPorId(id);
        validarLecturaTurno(turno);
        return mapTurno(turno);
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> listarPorPaciente(Long pacienteId) {
        validarAccesoPaciente(pacienteId);
        return turnoRepository.findByPacienteIdOrderByFechaHoraInicioDesc(pacienteId).stream().map(this::mapTurno).toList();
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> listarHistoriaClinica(Long usuarioId) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isPatient() && !Objects.equals(user.usuarioId(), usuarioId)) {
            throw new AccessDeniedException("No podés consultar la historia clínica de otro paciente");
        }
        if (user.isSecretary()) {
            throw new AccessDeniedException("Secretaría no puede ver el detalle de historia clínica médica");
        }
        if (user.isProfessional()) {
            Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado para el usuario: " + usuarioId));
            if (!turnoRepository.existsByPacienteIdAndProfesionalId(paciente.getId(), user.profesionalId())) {
                throw new AccessDeniedException("No tenés relación asistencial con este paciente");
            }
        }
        return turnoRepository.findByPacienteUsuario_IdAndEstadoOrderByFechaHoraInicioDesc(usuarioId, EstadoTurno.ATENDIDO).stream().map(this::mapTurno).toList();
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadSlotResponse> listarDisponibilidad(Long profesionalInstitucionId) {
        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(profesionalInstitucionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada con id: " + profesionalInstitucionId));
        validarSedeActiva(pi);

        LocalDate today = appClock.today();
        LocalDateTime now = appClock.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(60).atTime(LocalTime.MAX);

        List<Turno> turnos = turnoRepository.findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(pi.getId(), from, to)
                .stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO && t.getEstado() != EstadoTurno.AUSENTE)
                .toList();
        List<AgendaBloqueo> bloqueos = agendaBloqueoRepository.findBloqueosActivos(pi.getId(), from, to);
        List<HorarioAtencion> horariosConfigurados = horarioAtencionRepository.findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(pi.getId());

        List<DisponibilidadSlotResponse> slots = new ArrayList<>();
        for (int dias = 0; dias <= 60; dias++) {
            LocalDate fecha = today.plusDays(dias);
            String dia = normalizarDia(fecha);
            for (HorarioAtencion horario : horariosConfigurados.stream().filter(h -> dia.equalsIgnoreCase(h.getDiaSemana())).toList()) {
                LocalTime cursor = horario.getHoraDesde();
                int duracion = duracion(horario);
                while (!cursor.plusMinutes(duracion).isAfter(horario.getHoraHasta())) {
                    LocalDateTime inicio = fecha.atTime(cursor);
                    LocalDateTime fin = inicio.plusMinutes(duracion);
                    if (inicio.isAfter(now)
                            && !existeTurnoSolapado(turnos, inicio, fin, null)
                            && !estaBloqueado(inicio, fin, bloqueos)) {
                        slots.add(new DisponibilidadSlotResponse(fecha.toString(), cursor.toString(), inicio.toString()));
                    }
                    cursor = cursor.plusMinutes(duracion);
                }
            }
        }
        return slots.stream().sorted(Comparator.comparing(DisponibilidadSlotResponse::getFechaHoraIso)).toList();
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> agendaProfesional(Long usuarioId, LocalDate fecha) {
        LocalDate base = fecha != null ? fecha : appClock.today();
        LocalDateTime from = base.atStartOfDay();
        LocalDateTime to = base.atTime(LocalTime.MAX);
        return turnoRepository.findAgendaProfesional(usuarioId, from, to).stream().map(this::mapTurno).toList();
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> agendaProfesionalRango(Long usuarioId, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new IllegalArgumentException("Faltan las fechas desde y hasta");
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha hasta no puede ser anterior a desde");
        if (desde.plusYears(1).isBefore(hasta)) throw new IllegalArgumentException("El rango máximo permitido es de un año");
        LocalDateTime from = desde.atStartOfDay();
        LocalDateTime to = hasta.atTime(LocalTime.MAX);
        return turnoRepository.findAgendaProfesional(usuarioId, from, to).stream().map(this::mapTurno).toList();
    }

    @Transactional(readOnly = true)
    public TurnoResponse proximoTurnoProfesional(Long usuarioId) {
        return turnoRepository.findFirstByProfesionalUsuario_IdAndFechaHoraInicioAfterAndEstadoInOrderByFechaHoraInicioAsc(
                        usuarioId, appClock.now(), List.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO, EstadoTurno.REPROGRAMADO))
                .map(this::mapTurno)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> historiaPorDni(String dni) {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        if (user.isProfessional()) {
            return turnoRepository.findHistoriaPorDniAndProfesionalId(dni, user.profesionalId()).stream().map(this::mapTurno).toList();
        }
        return turnoRepository.findHistoriaPorDni(dni).stream().map(this::mapTurno).toList();
    }

    @Transactional
    public TurnoResponse solicitar(TurnoSolicitudRequest request) {
        validarActorParaSolicitar(request.getPacienteId());
        Paciente paciente = pacienteRepository.findById(request.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + request.getPacienteId()));
        Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + request.getProfesionalId()));
        ProfesionalInstitucion pi = resolverProfesionalInstitucion(profesional.getId(), request.getProfesionalInstitucionId());
        Especialidad especialidad = resolverEspecialidad(profesional, request.getEspecialidadId());
        int duracion = validarDisponibilidad(pi.getId(), request.getFechaHora(), null, especialidad.getId());

        Turno turno = new Turno();
        turno.setFechaHoraInicio(request.getFechaHora());
        turno.setFechaHoraFin(request.getFechaHora().plusMinutes(duracion));
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
            adjunto.setMimeType(defaultIfBlank(request.getDocumentacionMimeType(), MIME_IMAGE_JPEG));
            adjunto.setContenidoBase64(request.getDocumentacionBase64());
            turnoAdjuntoRepository.save(adjunto);
            guardado.getAdjuntos().add(adjunto);
        }

        TurnoResponse response = mapTurno(guardado);
        String emailPaciente = guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null
                ? guardado.getPaciente().getUsuario().getEmail()
                : null;
        verificationDispatchService.enviarConfirmacionTurno(response, emailPaciente);
        auditService.registrar("TURNO_ALTA", TABLA_TURNOS, guardado.getId(), null, "Turno creado para " + guardado.getFechaHoraInicio());
        return response;
    }

    @Transactional
    public FileUploadResponse adjuntarDocumentacion(Long turnoId, MultipartFile file) {
        return adjuntarDocumentacion(turnoId, file, null);
    }

    @Transactional
    public FileUploadResponse adjuntarDocumentacion(Long turnoId, MultipartFile file, String tipoDocumento) {
        Turno turno = obtenerEntidadPorId(turnoId);
        validarPacienteOAdministrativo(turno);
        MediaFileService.StoredFile stored = mediaFileService.storeMedicalDocument(file, TABLA_TURNOS, turnoId);

        TurnoAdjunto adjunto = new TurnoAdjunto();
        adjunto.setTurno(turno);
        adjunto.setNombreArchivo(stored.originalName());
        adjunto.setMimeType(stored.mimeType());
        adjunto.setStorageMimeType(stored.mimeType());
        adjunto.setStoragePath(stored.relativePath());
        adjunto.setOriginalSizeBytes(stored.originalSizeBytes());
        adjunto.setCompressedSizeBytes(stored.storedSizeBytes());
        adjunto.setTipoDocumento(normalizar(tipoDocumento));
        TurnoAdjunto guardado = turnoAdjuntoRepository.save(adjunto);
        turno.getAdjuntos().add(guardado);
        auditService.registrar("TURNO_ADJUNTO_ALTA", "turno_adjuntos", guardado.getId(), null, "Documento adjuntado al turno " + turnoId);

        return new FileUploadResponse(
                guardado.getId(),
                guardado.getNombreArchivo(),
                guardado.getMimeType(),
                guardado.getOriginalSizeBytes(),
                guardado.getCompressedSizeBytes(),
                adjuntoUrl(guardado),
                "Documentación adjuntada correctamente"
        );
    }

    @Transactional(readOnly = true)
    public TurnoAdjunto obtenerAdjunto(Long adjuntoId) {
        TurnoAdjunto adjunto = turnoAdjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado con id: " + adjuntoId));
        validarLecturaTurno(adjunto.getTurno());
        return adjunto;
    }

    @Transactional
    public TurnoResponse reprogramar(Long turnoId, TurnoReprogramacionRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        validarReprogramacion(turno);
        if (turno.getFechaHoraInicio().isBefore(appClock.now())) {
            throw new IllegalArgumentException("Solo se pueden reprogramar turnos próximos");
        }
        Profesional profesional = profesionalRepository.findById(request.getProfesionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + request.getProfesionalId()));
        ProfesionalInstitucion pi = resolverProfesionalInstitucion(profesional.getId(), request.getProfesionalInstitucionId());
        Especialidad especialidad = resolverEspecialidad(profesional, request.getEspecialidadId() != null ? request.getEspecialidadId() : turno.getEspecialidad().getId());

        int duracion = validarDisponibilidad(pi.getId(), request.getFechaHora(), turnoId, especialidad.getId());
        turno.setFechaHoraInicio(request.getFechaHora());
        turno.setFechaHoraFin(request.getFechaHora().plusMinutes(duracion));
        turno.setProfesional(profesional);
        turno.setProfesionalInstitucion(pi);
        turno.setEspecialidad(especialidad);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        Turno guardado = turnoRepository.save(turno);
        auditService.registrar("TURNO_REPROGRAMADO", TABLA_TURNOS, turnoId, null, "Turno reprogramado para " + request.getFechaHora());
        return mapTurno(guardado);
    }

    @Transactional
    public TurnoResponse actualizarEstado(Long turnoId, TurnoEstadoUpdateRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        validarCambioEstado(turno);
        if (request.getEstado() == null) throw new IllegalArgumentException("Falta estado");
        turno.setEstado(request.getEstado());
        TurnoResponse response = mapTurno(turnoRepository.save(turno));
        auditService.registrar("TURNO_ESTADO", TABLA_TURNOS, turnoId, null, "Estado actualizado a " + request.getEstado());
        if (request.getEstado() == EstadoTurno.CANCELADO) {
            listaEsperaService.notificarPrimerPendienteCompatible(turno, response);
        }
        return response;
    }

    @Transactional
    public TurnoResponse confirmarAsistencia(Long turnoId) {
        Turno turno = obtenerEntidadPorId(turnoId);
        validarPacienteOAdministrativo(turno);
        if (turno.getEstado() == EstadoTurno.CANCELADO || turno.getEstado() == EstadoTurno.AUSENTE) {
            throw new IllegalArgumentException("No se puede confirmar asistencia de un turno cancelado o ausente");
        }
        turno.setAsistenciaConfirmada(true);
        turno.setAsistenciaConfirmadaEn(appClock.now());
        turno.setEstado(EstadoTurno.CONFIRMADO);
        TurnoResponse response = mapTurno(turnoRepository.save(turno));
        auditService.registrar("TURNO_ASISTENCIA_CONFIRMADA", TABLA_TURNOS, turnoId, null, "Paciente confirmó asistencia");
        return response;
    }

    @Transactional
    public TurnoResponse cargarDetalleConsulta(Long turnoId, DetalleConsultaRequest request) {
        Turno turno = obtenerEntidadPorId(turnoId);
        validarAtencionMedica(turno);
        Consulta consulta = turno.getConsulta();
        if (consulta == null) {
            consulta = new Consulta();
            consulta.setTurno(turno);
        }
        consulta.setFechaAtencion(appClock.now());
        consulta.setMotivoConsulta(normalizar(request.getMotivoConsulta()));
        consulta.setEnfermedadActual(normalizar(request.getEnfermedadActual()));
        consulta.setAntecedenteEnfermedadActual(normalizar(request.getAntecedenteEnfermedadActual()));
        consulta.setAntecedentesPersonales(normalizar(request.getAntecedentesPersonales()));
        consulta.setAntecedentesFamiliares(normalizar(request.getAntecedentesFamiliares()));
        consulta.setMedicacionActual(normalizar(request.getMedicacionActual()));
        consulta.setAlergias(normalizar(request.getAlergias()));
        consulta.setHabitos(normalizar(request.getHabitos()));
        consulta.setHallazgosExamenFisico(normalizar(request.getHallazgosExamenFisico()));
        consulta.setDiagnostico(normalizar(request.getDiagnostico()));
        consulta.setConducta(normalizar(request.getConducta()));
        consultaRepository.save(consulta);
        turno.setConsulta(consulta);
        turno.setEstado(EstadoTurno.ATENDIDO);
        Turno guardado = turnoRepository.save(turno);
        auditService.registrar("TURNO_ATENDIDO", TABLA_TURNOS, turnoId, null, "Consulta cargada por profesional");
        return mapTurno(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Turno turno = obtenerEntidadPorId(id);
        currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY);
        turno.setEstado(EstadoTurno.CANCELADO);
        turnoRepository.save(turno);
        auditService.registrar("TURNO_CANCELADO", TABLA_TURNOS, id, null, "Turno cancelado desde endpoint de eliminación lógica");
    }

    private Turno obtenerEntidadPorId(Long id) {
        return turnoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + id));
    }

    private int validarDisponibilidad(Long profesionalInstitucionId, LocalDateTime fechaHora, Long turnoIdIgnorado, Long especialidadId) {
        if (fechaHora == null) throw new IllegalArgumentException("Falta fecha y hora");
        if (!fechaHora.isAfter(appClock.now())) throw new IllegalArgumentException("El turno debe ser a futuro");

        ProfesionalInstitucion pi = profesionalInstitucionRepository.findById(profesionalInstitucionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada"));
        validarSedeActiva(pi);

        String dia = normalizarDia(fechaHora.toLocalDate());
        List<HorarioAtencion> horariosDia = horarioAtencionRepository.findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(profesionalInstitucionId)
                .stream()
                .filter(h -> dia.equalsIgnoreCase(h.getDiaSemana()))
                .filter(h -> especialidadId == null || h.getEspecialidad().getId().equals(especialidadId))
                .toList();
        if (horariosDia.isEmpty()) {
            throw new IllegalArgumentException("El profesional no atiende ese día o esa especialidad en la sede seleccionada");
        }

        int duracion = horariosDia.stream()
                .filter(h -> encajaEnHorario(fechaHora.toLocalTime(), h))
                .map(this::duracion)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El horario elegido no pertenece a la agenda configurada del profesional"));

        LocalDateTime fin = fechaHora.plusMinutes(duracion);
        List<Turno> turnosDelDia = turnoRepository.findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(
                        profesionalInstitucionId, fechaHora.toLocalDate().atStartOfDay(), fechaHora.toLocalDate().atTime(LocalTime.MAX))
                .stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO && t.getEstado() != EstadoTurno.AUSENTE)
                .toList();
        if (existeTurnoSolapado(turnosDelDia, fechaHora, fin, turnoIdIgnorado)) {
            throw new IllegalArgumentException("Ese horario ya no está disponible");
        }
        List<AgendaBloqueo> bloqueos = agendaBloqueoRepository.findBloqueosActivos(profesionalInstitucionId, fechaHora, fin);
        if (estaBloqueado(fechaHora, fin, bloqueos)) {
            throw new IllegalArgumentException("Ese horario está bloqueado por el profesional");
        }
        return duracion;
    }

    private boolean encajaEnHorario(LocalTime hora, HorarioAtencion h) {
        int duracion = duracion(h);
        LocalTime fin = hora.plusMinutes(duracion);
        if (hora.isBefore(h.getHoraDesde()) || fin.isAfter(h.getHoraHasta())) return false;
        long diff = java.time.Duration.between(h.getHoraDesde(), hora).toMinutes();
        return diff >= 0 && diff % duracion == 0;
    }

    private int duracion(HorarioAtencion h) {
        return h.getDuracionTurnoMin() != null && h.getDuracionTurnoMin() > 0 ? h.getDuracionTurnoMin() : DURACION_MINUTOS;
    }

    private boolean existeTurnoSolapado(List<Turno> turnos, LocalDateTime inicio, LocalDateTime fin, Long turnoIdIgnorado) {
        return turnos.stream()
                .filter(t -> turnoIdIgnorado == null || !t.getId().equals(turnoIdIgnorado))
                .anyMatch(t -> intervalosSolapan(inicio, fin, t.getFechaHoraInicio(), t.getFechaHoraFin()));
    }

    private boolean estaBloqueado(LocalDateTime inicio, LocalDateTime fin, List<AgendaBloqueo> bloqueos) {
        return bloqueos.stream().anyMatch(b -> intervalosSolapan(inicio, fin, b.getFechaDesde(), b.getFechaHasta()));
    }

    private boolean intervalosSolapan(LocalDateTime aDesde, LocalDateTime aHasta, LocalDateTime bDesde, LocalDateTime bHasta) {
        return aDesde.isBefore(bHasta) && aHasta.isAfter(bDesde);
    }

    private ProfesionalInstitucion resolverProfesionalInstitucion(Long profesionalId, Long profesionalInstitucionId) {
        ProfesionalInstitucion pi;
        if (profesionalInstitucionId != null) {
            pi = profesionalInstitucionRepository.findById(profesionalInstitucionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede profesional no encontrada con id: " + profesionalInstitucionId));
            if (!pi.getProfesional().getId().equals(profesionalId)) {
                throw new IllegalArgumentException("La sede seleccionada no pertenece al profesional elegido");
            }
        } else {
            pi = profesionalInstitucionRepository.findByProfesionalIdAndActivoTrue(profesionalId).stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El profesional no tiene sedes activas configuradas"));
        }
        validarSedeActiva(pi);
        return pi;
    }

    private void validarSedeActiva(ProfesionalInstitucion pi) {
        if (pi == null || !Boolean.TRUE.equals(pi.getActivo()) || pi.getProfesional() == null || !Boolean.TRUE.equals(pi.getProfesional().getActivo()) || pi.getInstitucion() == null || !Boolean.TRUE.equals(pi.getInstitucion().getActiva())) {
            throw new IllegalArgumentException("La sede/profesional/institución no está activa");
        }
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

    private void validarActorParaSolicitar(Long pacienteId) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isPatient() && !Objects.equals(user.pacienteId(), pacienteId)) {
            throw new AccessDeniedException("No podés crear turnos para otro paciente");
        }
        if (user.isProfessional()) {
            throw new AccessDeniedException("Un profesional no puede crear turnos desde este endpoint");
        }
    }

    private void validarAccesoPaciente(Long pacienteId) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && Objects.equals(user.pacienteId(), pacienteId)) return;
        throw new AccessDeniedException("No podés consultar turnos de otro paciente");
    }

    private void validarLecturaTurno(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && turno.getPaciente() != null && Objects.equals(turno.getPaciente().getId(), user.pacienteId())) return;
        if (user.isProfessional() && turno.getProfesional() != null && Objects.equals(turno.getProfesional().getId(), user.profesionalId())) return;
        throw new AccessDeniedException("No tenés acceso a este turno");
    }

    private void validarPacienteOAdministrativo(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && turno.getPaciente() != null && Objects.equals(turno.getPaciente().getId(), user.pacienteId())) return;
        throw new AccessDeniedException("No podés operar sobre este turno");
    }

    private void validarReprogramacion(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && turno.getPaciente() != null && Objects.equals(turno.getPaciente().getId(), user.pacienteId())) return;
        if (user.isProfessional() && turno.getProfesional() != null && Objects.equals(turno.getProfesional().getId(), user.profesionalId())) return;
        throw new AccessDeniedException("No podés reprogramar este turno");
    }

    private void validarCambioEstado(Turno turno) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isProfessional() && turno.getProfesional() != null && Objects.equals(turno.getProfesional().getId(), user.profesionalId())) return;
        throw new AccessDeniedException("No podés cambiar el estado de este turno");
    }

    private void validarAtencionMedica(Turno turno) {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        if (user.isAdmin()) return;
        if (turno.getProfesional() != null && Objects.equals(turno.getProfesional().getId(), user.profesionalId())) return;
        throw new AccessDeniedException("No podés atender turnos asignados a otro profesional");
    }

    private TurnoResponse mapTurno(Turno turno) {
        TurnoAdjunto adjunto = primerAdjunto(turno);
        Consulta consulta = turno.getConsulta();
        return new TurnoResponse(
                turno.getId(),
                turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(),
                turno.getEstado(),
                turno.getObservacionesPaciente(),
                institucionId(turno),
                institucionNombre(turno),
                institucionDireccion(turno),
                pacienteId(turno),
                pacienteNombre(turno),
                pacienteApellido(turno),
                pacienteDni(turno),
                profesionalId(turno),
                profesionalInstitucionId(turno),
                profesionalNombre(turno),
                profesionalApellido(turno),
                especialidadId(turno),
                especialidadNombre(turno),
                telefonoContacto(turno),
                adjuntoNombre(adjunto),
                adjuntoMimeType(adjunto),
                adjuntoContenidoBase64(adjunto),
                adjuntoId(adjunto),
                adjuntoUrl(adjunto),
                adjuntoCompressedSizeBytes(adjunto),
                turno.getAsistenciaConfirmada(),
                turno.getAsistenciaConfirmadaEn(),
                turno.getRecordatorioTresHorasEnviado(),
                motivoConsulta(consulta),
                enfermedadActual(consulta),
                antecedenteEnfermedadActual(consulta),
                antecedentesPersonales(consulta),
                antecedentesFamiliares(consulta),
                medicacionActual(consulta),
                alergias(consulta),
                habitos(consulta),
                hallazgosExamenFisico(consulta),
                diagnostico(consulta),
                conducta(consulta)
        );
    }

    private TurnoAdjunto primerAdjunto(Turno turno) {
        return turno.getAdjuntos() != null && !turno.getAdjuntos().isEmpty() ? turno.getAdjuntos().get(0) : null;
    }

    private Long institucionId(Turno turno) {
        return turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getId() : null;
    }

    private String institucionNombre(Turno turno) {
        return turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getNombre() : null;
    }

    private String institucionDireccion(Turno turno) {
        return turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getInstitucion().getDireccion() : null;
    }

    private Long pacienteId(Turno turno) {
        return turno.getPaciente() != null ? turno.getPaciente().getId() : null;
    }

    private String pacienteNombre(Turno turno) {
        return turno.getPaciente() != null ? turno.getPaciente().getNombre() : null;
    }

    private String pacienteApellido(Turno turno) {
        return turno.getPaciente() != null ? turno.getPaciente().getApellido() : null;
    }

    private String pacienteDni(Turno turno) {
        return turno.getPaciente() != null ? turno.getPaciente().getDni() : null;
    }

    private Long profesionalId(Turno turno) {
        return turno.getProfesional() != null ? turno.getProfesional().getId() : null;
    }

    private Long profesionalInstitucionId(Turno turno) {
        return turno.getProfesionalInstitucion() != null ? turno.getProfesionalInstitucion().getId() : null;
    }

    private String profesionalNombre(Turno turno) {
        return turno.getProfesional() != null ? turno.getProfesional().getNombre() : null;
    }

    private String profesionalApellido(Turno turno) {
        return turno.getProfesional() != null ? turno.getProfesional().getApellido() : null;
    }

    private Long especialidadId(Turno turno) {
        return turno.getEspecialidad() != null ? turno.getEspecialidad().getId() : null;
    }

    private String especialidadNombre(Turno turno) {
        return turno.getEspecialidad() != null ? turno.getEspecialidad().getNombre() : null;
    }

    private String adjuntoNombre(TurnoAdjunto adjunto) {
        return adjunto != null ? adjunto.getNombreArchivo() : null;
    }

    private String adjuntoMimeType(TurnoAdjunto adjunto) {
        return adjunto != null ? adjunto.getMimeType() : null;
    }

    private String adjuntoContenidoBase64(TurnoAdjunto adjunto) {
        return adjunto != null ? adjunto.getContenidoBase64() : null;
    }

    private Long adjuntoId(TurnoAdjunto adjunto) {
        return adjunto != null ? adjunto.getId() : null;
    }

    private Long adjuntoCompressedSizeBytes(TurnoAdjunto adjunto) {
        return adjunto != null ? adjunto.getCompressedSizeBytes() : null;
    }

    private String motivoConsulta(Consulta consulta) {
        return consulta != null ? consulta.getMotivoConsulta() : null;
    }

    private String enfermedadActual(Consulta consulta) {
        return consulta != null ? consulta.getEnfermedadActual() : null;
    }

    private String antecedenteEnfermedadActual(Consulta consulta) {
        return consulta != null ? consulta.getAntecedenteEnfermedadActual() : null;
    }

    private String antecedentesPersonales(Consulta consulta) {
        return consulta != null ? consulta.getAntecedentesPersonales() : null;
    }

    private String antecedentesFamiliares(Consulta consulta) {
        return consulta != null ? consulta.getAntecedentesFamiliares() : null;
    }

    private String medicacionActual(Consulta consulta) {
        return consulta != null ? consulta.getMedicacionActual() : null;
    }

    private String alergias(Consulta consulta) {
        return consulta != null ? consulta.getAlergias() : null;
    }

    private String habitos(Consulta consulta) {
        return consulta != null ? consulta.getHabitos() : null;
    }

    private String hallazgosExamenFisico(Consulta consulta) {
        return consulta != null ? consulta.getHallazgosExamenFisico() : null;
    }

    private String diagnostico(Consulta consulta) {
        return consulta != null ? consulta.getDiagnostico() : null;
    }

    private String conducta(Consulta consulta) {
        return consulta != null ? consulta.getConducta() : null;
    }

    private String telefonoContacto(Turno turno) {
        if (turno.getProfesionalInstitucion() != null && turno.getProfesionalInstitucion().getTelefonoEnSede() != null) {
            return turno.getProfesionalInstitucion().getTelefonoEnSede();
        }
        if (turno.getProfesional() != null) {
            return turno.getProfesional().getTelefono();
        }
        return null;
    }

    private String adjuntoUrl(TurnoAdjunto adjunto) {
        if (adjunto == null || adjunto.getStoragePath() == null || adjunto.getStoragePath().isBlank()) return null;
        return "/api/turnos/adjuntos/" + adjunto.getId() + "/archivo";
    }

    private String normalizarDia(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
