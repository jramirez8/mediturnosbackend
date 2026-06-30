package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.AgendaBloqueoRequest;
import com.ramirez.mediturnosback.dto.HorarioAtencionRequest;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgendaServiceTest {

    private HorarioAtencionRepository horarioRepository;
    private AgendaBloqueoRepository bloqueoRepository;
    private ProfesionalInstitucionRepository piRepository;
    private EspecialidadRepository especialidadRepository;
    private AuditService auditService;
    private CurrentUserService currentUserService;
    private AgendaService service;
    private ProfesionalInstitucion pi;
    private Especialidad especialidad;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioAtencionRepository.class);
        bloqueoRepository = mock(AgendaBloqueoRepository.class);
        piRepository = mock(ProfesionalInstitucionRepository.class);
        especialidadRepository = mock(EspecialidadRepository.class);
        auditService = mock(AuditService.class);
        currentUserService = mock(CurrentUserService.class);
        service = new AgendaService(horarioRepository, bloqueoRepository, piRepository, especialidadRepository, auditService, currentUserService);

        Usuario owner = new Usuario();
        owner.setId(7L);
        owner.setEmail("medico@mediturnos.net.ar");
        owner.setRol(RolUsuario.PROFESSIONAL);

        especialidad = new Especialidad();
        especialidad.setId(20L);
        especialidad.setNombre("Cardiología");

        Profesional profesional = new Profesional();
        profesional.setId(30L);
        profesional.setUsuario(owner);
        profesional.setEspecialidades(Set.of(especialidad));

        pi = new ProfesionalInstitucion();
        pi.setId(10L);
        pi.setProfesional(profesional);

        when(piRepository.findById(10L)).thenReturn(Optional.of(pi));
        when(especialidadRepository.findById(20L)).thenReturn(Optional.of(especialidad));
        when(currentUserService.requireAnyRole(any(RolUsuario[].class)))
                .thenReturn(new AuthenticatedUser(1L, null, null, RolUsuario.ADMIN, "admin@mediturnos.net.ar"));
        when(horarioRepository.save(any(HorarioAtencion.class))).thenAnswer(invocation -> {
            HorarioAtencion horario = invocation.getArgument(0);
            if (horario.getId() == null) horario.setId(100L);
            return horario;
        });
        when(bloqueoRepository.save(any(AgendaBloqueo.class))).thenAnswer(invocation -> {
            AgendaBloqueo bloqueo = invocation.getArgument(0);
            if (bloqueo.getId() == null) bloqueo.setId(200L);
            return bloqueo;
        });
    }

    @Test
    void creaHorarioNormalizandoDiaYMapeandoRespuesta() {
        HorarioAtencionRequest request = requestBase();
        request.setDiaSemana("miércoles");

        var response = service.crearHorario(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDiaSemana()).isEqualTo("MIERCOLES");
        assertThat(response.getEspecialidad()).isEqualTo("Cardiología");
        assertThat(response.getDuracionTurnoMin()).isEqualTo(30);
        assertThat(response.getActivo()).isTrue();
        verify(auditService).registrar(eq("AGENDA_HORARIO_ALTA"), eq("horarios_atencion"), eq(100L), isNull(), anyString());
    }

    @Test
    void creaHorarioConDuracionYActivoPorDefecto() {
        HorarioAtencionRequest request = requestBase();
        request.setDuracionTurnoMin(null);
        request.setActivo(null);

        var response = service.crearHorario(request);

        assertThat(response.getDuracionTurnoMin()).isEqualTo(30);
        assertThat(response.getActivo()).isTrue();
    }

    @Test
    void aceptaSabadoComoExcepcionConfigurable() {
        HorarioAtencionRequest request = requestBase();
        request.setDiaSemana("satURDAY");

        assertThat(service.crearHorario(request).getDiaSemana()).isEqualTo("SABADO");
    }

    @Test
    void rechazaRangoInvertido() {
        HorarioAtencionRequest request = requestBase();
        request.setHoraDesde(LocalTime.of(13, 0));
        request.setHoraHasta(LocalTime.of(9, 0));

        assertThatThrownBy(() -> service.crearHorario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hora desde/hasta");
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void rechazaDuracionNoPermitida() {
        HorarioAtencionRequest request = requestBase();
        request.setDuracionTurnoMin(17);

        assertThatThrownBy(() -> service.crearHorario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duración inválida");
    }

    @Test
    void rechazaRangoMasCortoQueElTurno() {
        HorarioAtencionRequest request = requestBase();
        request.setHoraDesde(LocalTime.of(9, 0));
        request.setHoraHasta(LocalTime.of(9, 20));
        request.setDuracionTurnoMin(30);

        assertThatThrownBy(() -> service.crearHorario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("más chico");
    }

    @Test
    void profesionalNoPuedeModificarAgendaAjena() {
        when(currentUserService.requireAnyRole(any(RolUsuario[].class)))
                .thenReturn(new AuthenticatedUser(99L, null, 44L, RolUsuario.PROFESSIONAL, "otro@mediturnos.net.ar"));

        assertThatThrownBy(() -> service.crearHorario(requestBase()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("otro profesional");
    }

    @Test
    void rechazaEspecialidadQueNoPerteneceAlProfesional() {
        Especialidad otra = new Especialidad();
        otra.setId(99L);
        otra.setNombre("Traumatología");
        when(especialidadRepository.findById(99L)).thenReturn(Optional.of(otra));
        HorarioAtencionRequest request = requestBase();
        request.setEspecialidadId(99L);

        assertThatThrownBy(() -> service.crearHorario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void listaHorariosActivos() {
        HorarioAtencion horario = new HorarioAtencion();
        horario.setId(5L);
        horario.setProfesionalInstitucion(pi);
        horario.setEspecialidad(especialidad);
        horario.setDiaSemana("LUNES");
        horario.setHoraDesde(LocalTime.of(9, 0));
        horario.setHoraHasta(LocalTime.of(13, 0));
        horario.setDuracionTurnoMin(30);
        horario.setActivo(true);
        when(horarioRepository.findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(10L))
                .thenReturn(List.of(horario));

        var result = service.horarios(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHoraDesde()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void desactivaHorarioSinBorrarlo() {
        HorarioAtencion horario = new HorarioAtencion();
        horario.setId(5L);
        horario.setProfesionalInstitucion(pi);
        horario.setActivo(true);
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(horario));
        when(horarioRepository.save(horario)).thenReturn(horario);

        service.desactivarHorario(5L);

        assertThat(horario.getActivo()).isFalse();
        verify(horarioRepository).save(horario);
        verify(auditService).registrar(eq("AGENDA_HORARIO_BAJA"), anyString(), eq(5L), isNull(), anyString());
    }

    @Test
    void creaBloqueoValidoYRecortaMotivo() {
        AgendaBloqueoRequest request = new AgendaBloqueoRequest();
        request.setProfesionalInstitucionId(10L);
        request.setFechaDesde(LocalDateTime.of(2026, Month.JULY, 1, 9, 0));
        request.setFechaHasta(LocalDateTime.of(2026, Month.JULY, 1, 13, 0));
        request.setMotivo("  Jornada académica  ");

        var result = service.crearBloqueo(request);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getMotivo()).isEqualTo("Jornada académica");
    }

    @Test
    void rechazaBloqueoSinRangoValido() {
        AgendaBloqueoRequest request = new AgendaBloqueoRequest();
        request.setProfesionalInstitucionId(10L);
        request.setFechaDesde(LocalDateTime.of(2026, Month.JULY, 2, 13, 0));
        request.setFechaHasta(LocalDateTime.of(2026, Month.JULY, 2, 9, 0));

        assertThatThrownBy(() -> service.crearBloqueo(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha desde/hasta");
    }


    @Test
    void actualizaHorarioExistenteConNuevosValores() {
        HorarioAtencion existente = new HorarioAtencion();
        existente.setId(5L);
        existente.setProfesionalInstitucion(pi);
        existente.setEspecialidad(especialidad);
        existente.setDiaSemana("LUNES");
        existente.setHoraDesde(LocalTime.of(9, 0));
        existente.setHoraHasta(LocalTime.of(13, 0));
        existente.setDuracionTurnoMin(30);
        existente.setActivo(true);
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(existente));

        HorarioAtencionRequest request = new HorarioAtencionRequest();
        request.setDiaSemana("viernes");
        request.setHoraDesde(LocalTime.of(10, 0));
        request.setHoraHasta(LocalTime.of(14, 0));
        request.setDuracionTurnoMin(45);

        var response = service.actualizarHorario(5L, request);

        assertThat(response.getDiaSemana()).isEqualTo("VIERNES");
        assertThat(response.getHoraDesde()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getDuracionTurnoMin()).isEqualTo(45);
        verify(auditService).registrar(eq("AGENDA_HORARIO_EDICION"), anyString(), eq(5L), isNull(), anyString());
    }

    @Test
    void actualizaHorarioPreservandoDuracionYActivoExistentes() {
        HorarioAtencion existente = new HorarioAtencion();
        existente.setId(5L);
        existente.setProfesionalInstitucion(pi);
        existente.setEspecialidad(especialidad);
        existente.setDiaSemana("LUNES");
        existente.setHoraDesde(LocalTime.of(9, 0));
        existente.setHoraHasta(LocalTime.of(13, 0));
        existente.setDuracionTurnoMin(45);
        existente.setActivo(false);
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(existente));

        HorarioAtencionRequest request = new HorarioAtencionRequest();

        var response = service.actualizarHorario(5L, request);

        assertThat(response.getDuracionTurnoMin()).isEqualTo(45);
        assertThat(response.getActivo()).isFalse();
    }

    @Test
    void usaPrimeraEspecialidadSiNoSeIndicaUna() {
        HorarioAtencionRequest request = requestBase();
        request.setEspecialidadId(null);

        assertThat(service.crearHorario(request).getEspecialidadId()).isEqualTo(20L);
    }

    @Test
    void rechazaDiaInvalido() {
        HorarioAtencionRequest request = requestBase();
        request.setDiaSemana("FERIADO");

        assertThatThrownBy(() -> service.crearHorario(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Día de semana inválido");
    }

    @Test
    void listaBloqueosConfigurados() {
        AgendaBloqueo bloqueo = new AgendaBloqueo();
        bloqueo.setId(8L);
        bloqueo.setProfesionalInstitucion(pi);
        bloqueo.setFechaDesde(LocalDateTime.of(2026, Month.JULY, 10, 9, 0));
        bloqueo.setFechaHasta(LocalDateTime.of(2026, Month.JULY, 10, 13, 0));
        bloqueo.setMotivo("Capacitación");
        when(bloqueoRepository.findByProfesionalInstitucionIdOrderByFechaDesdeAsc(10L)).thenReturn(List.of(bloqueo));

        var result = service.bloqueos(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMotivo()).isEqualTo("Capacitación");
    }

    @Test
    void actualizaBloqueoExistente() {
        AgendaBloqueo bloqueo = new AgendaBloqueo();
        bloqueo.setId(8L);
        bloqueo.setProfesionalInstitucion(pi);
        bloqueo.setFechaDesde(LocalDateTime.of(2026, Month.JULY, 10, 9, 0));
        bloqueo.setFechaHasta(LocalDateTime.of(2026, Month.JULY, 10, 13, 0));
        bloqueo.setMotivo("Original");
        when(bloqueoRepository.findById(8L)).thenReturn(Optional.of(bloqueo));

        AgendaBloqueoRequest request = new AgendaBloqueoRequest();
        request.setFechaDesde(LocalDateTime.of(2026, Month.JULY, 11, 10, 0));
        request.setFechaHasta(LocalDateTime.of(2026, Month.JULY, 11, 12, 0));
        request.setMotivo("  Reunión  ");

        var result = service.actualizarBloqueo(8L, request);

        assertThat(result.getMotivo()).isEqualTo("Reunión");
        assertThat(result.getFechaDesde()).isEqualTo(LocalDateTime.of(2026, Month.JULY, 11, 10, 0));
        verify(auditService).registrar(eq("AGENDA_BLOQUEO_EDICION"), anyString(), eq(8L), isNull(), anyString());
    }

    @Test
    void eliminaBloqueoExistente() {
        AgendaBloqueo bloqueo = new AgendaBloqueo();
        bloqueo.setId(8L);
        bloqueo.setProfesionalInstitucion(pi);
        when(bloqueoRepository.findById(8L)).thenReturn(Optional.of(bloqueo));

        service.eliminarBloqueo(8L);

        verify(bloqueoRepository).delete(bloqueo);
        verify(auditService).registrar(eq("AGENDA_BLOQUEO_BAJA"), anyString(), eq(8L), isNull(), anyString());
    }

    private HorarioAtencionRequest requestBase() {
        HorarioAtencionRequest request = new HorarioAtencionRequest();
        request.setProfesionalInstitucionId(10L);
        request.setEspecialidadId(20L);
        request.setDiaSemana("LUNES");
        request.setHoraDesde(LocalTime.of(9, 0));
        request.setHoraHasta(LocalTime.of(13, 0));
        request.setDuracionTurnoMin(30);
        request.setActivo(true);
        return request;
    }
}
