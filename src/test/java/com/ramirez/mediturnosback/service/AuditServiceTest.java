package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.model.AuditLog;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.repository.AuditLogRepository;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    private AuditLogRepository repository;
    private CurrentUserService currentUserService;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new AuditService(repository, currentUserService);
    }

    @Test
    void registraActorAutenticadoReal() {
        AuthenticatedUser actor = new AuthenticatedUser(8L, null, 21L, RolUsuario.PROFESSIONAL, "jlopez@mediturnos.net.ar");
        when(currentUserService.optional()).thenReturn(Optional.of(actor));

        service.registrar("TURNO_ATENDIDO", "turnos", 44L, "ignorado", "Consulta finalizada");

        AuditLog log = capturarGuardado();
        assertThat(log.getActor()).contains("jlopez@mediturnos.net.ar");
        assertThat(log.getActorUsuarioId()).isEqualTo(8L);
        assertThat(log.getActorRol()).isEqualTo("PROFESSIONAL");
        assertThat(log.getActorEmail()).isEqualTo("jlopez@mediturnos.net.ar");
        assertThat(log.getEntidadId()).isEqualTo(44L);
    }

    @Test
    void usaActorExplicitoCuandoNoHaySesion() {
        when(currentUserService.optional()).thenReturn(Optional.empty());

        service.registrar("IMPORTACION", "usuarios", null, "proceso-nocturno", "Importación");

        AuditLog log = capturarGuardado();
        assertThat(log.getActor()).isEqualTo("proceso-nocturno");
        assertThat(log.getActorRol()).isEqualTo("SYSTEM");
    }

    @Test
    void registrarSistemaNoConsultaUsuarioActual() {
        service.registrarSistema("LOGIN_ERROR", "usuarios", 5L, "Clave inválida");

        AuditLog log = capturarGuardado();
        assertThat(log.getActor()).isEqualTo("sistema");
        assertThat(log.getActorRol()).isEqualTo("SYSTEM");
        verifyNoInteractions(currentUserService);
    }

    @Test
    void ultimosMapeaRespuesta() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAccion("USUARIO_ALTA");
        log.setEntidad("usuarios");
        log.setEntidadId(9L);
        log.setActor("admin");
        log.setActorUsuarioId(2L);
        log.setActorRol("ADMIN");
        log.setActorEmail("admin@mediturnos.net.ar");
        log.setDetalle("Paciente creado");
        log.setCreadoEn(LocalDateTime.of(2026, Month.JUNE, 17, 12, 0));
        when(repository.findTop100ByOrderByCreadoEnDesc()).thenReturn(List.of(log));

        var result = service.ultimos();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccion()).isEqualTo("USUARIO_ALTA");
        assertThat(result.get(0).getActorEmail()).isEqualTo("admin@mediturnos.net.ar");
    }

    private AuditLog capturarGuardado() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
