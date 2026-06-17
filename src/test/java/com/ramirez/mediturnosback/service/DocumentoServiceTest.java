package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.PacienteDocumentoRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.util.AppClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentoServiceTest {

    private PacienteDocumentoRepository documentoRepository;
    private PacienteRepository pacienteRepository;
    private TurnoRepository turnoRepository;
    private MediaFileService mediaFileService;
    private CurrentUserService currentUserService;
    private AuditService auditService;
    private AppClock appClock;
    private DocumentoService service;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(PacienteDocumentoRepository.class);
        pacienteRepository = mock(PacienteRepository.class);
        turnoRepository = mock(TurnoRepository.class);
        mediaFileService = mock(MediaFileService.class);
        currentUserService = mock(CurrentUserService.class);
        auditService = mock(AuditService.class);
        appClock = mock(AppClock.class);
        service = new DocumentoService(documentoRepository, pacienteRepository, turnoRepository, mediaFileService, currentUserService, auditService, appClock);

        paciente = new Paciente();
        paciente.setId(3L);
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
        when(pacienteRepository.findById(3L)).thenReturn(Optional.of(paciente));
    }

    @Test
    void pacienteListaSoloDocumentosPropiosNoArchivados() {
        AuthenticatedUser user = new AuthenticatedUser(1L, 3L, null, RolUsuario.PATIENT, "ana@x.com");
        when(currentUserService.requireAnyRole(RolUsuario.PATIENT)).thenReturn(user);
        when(currentUserService.requireUser()).thenReturn(user);
        when(documentoRepository.findByPacienteIdAndArchivadoFalseOrderByCreadoEnDesc(3L))
                .thenReturn(List.of(documento(11L, false)));

        var result = service.listarPropios();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPacienteId()).isEqualTo(3L);
        assertThat(result.get(0).getUrl()).isEqualTo("/api/documentos/11/archivo");
    }

    @Test
    void adminPuedeIncluirArchivados() {
        when(currentUserService.requireUser())
                .thenReturn(new AuthenticatedUser(1L, null, null, RolUsuario.ADMIN, "admin@x.com"));
        when(documentoRepository.findByPacienteIdOrderByCreadoEnDesc(3L))
                .thenReturn(List.of(documento(12L, true)));

        var result = service.listarPorPaciente(3L, true);

        assertThat(result).singleElement().extracting("archivado").isEqualTo(true);
        verify(documentoRepository).findByPacienteIdOrderByCreadoEnDesc(3L);
    }

    @Test
    void pacienteNoPuedeVerDocumentosAjenos() {
        when(currentUserService.requireUser())
                .thenReturn(new AuthenticatedUser(1L, 99L, null, RolUsuario.PATIENT, "otro@x.com"));

        assertThatThrownBy(() -> service.listarPorPaciente(3L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("acceder");
    }

    @Test
    void medicoConTurnoRelacionadoPuedeVerDocumentos() {
        when(currentUserService.requireUser())
                .thenReturn(new AuthenticatedUser(1L, null, 7L, RolUsuario.PROFESSIONAL, "medico@x.com"));
        when(turnoRepository.existsByPacienteIdAndProfesionalId(3L, 7L)).thenReturn(true);
        when(documentoRepository.findByPacienteIdAndArchivadoFalseOrderByCreadoEnDesc(3L)).thenReturn(List.of());

        assertThat(service.listarPorPaciente(3L, false)).isEmpty();
    }

    @Test
    void medicoSinRelacionNoPuedeArchivar() {
        PacienteDocumento doc = documento(11L, false);
        when(documentoRepository.findById(11L)).thenReturn(Optional.of(doc));
        AuthenticatedUser medico = new AuthenticatedUser(1L, null, 7L, RolUsuario.PROFESSIONAL, "medico@x.com");
        when(currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY, RolUsuario.PROFESSIONAL)).thenReturn(medico);
        when(turnoRepository.existsByPacienteIdAndProfesionalId(3L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.archivar(11L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("archivar");
    }

    @Test
    void secretariaPuedeArchivarYQuedaAuditado() {
        PacienteDocumento doc = documento(11L, false);
        when(documentoRepository.findById(11L)).thenReturn(Optional.of(doc));
        when(currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY, RolUsuario.PROFESSIONAL))
                .thenReturn(new AuthenticatedUser(2L, null, null, RolUsuario.SECRETARY, "secretaria@x.com"));
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 20, 0);
        when(appClock.now()).thenReturn(now);
        when(documentoRepository.save(doc)).thenReturn(doc);

        var result = service.archivar(11L);

        assertThat(result.getArchivado()).isTrue();
        assertThat(doc.getArchivadoEn()).isEqualTo(now);
        verify(auditService).registrar(eq("DOCUMENTO_ARCHIVADO"), anyString(), eq(11L), isNull(), anyString());
    }

    @Test
    void subeDocumentoAsociadoAPacienteYTurno() {
        Turno turno = new Turno();
        turno.setId(50L);
        turno.setPaciente(paciente);
        when(turnoRepository.findById(50L)).thenReturn(Optional.of(turno));
        when(currentUserService.requireUser())
                .thenReturn(new AuthenticatedUser(2L, null, null, RolUsuario.SECRETARY, "secretaria@x.com"));
        when(mediaFileService.storeMedicalDocument(any(), eq("pacientes/documentos"), eq(3L)))
                .thenReturn(new MediaFileService.StoredFile("placa.jpg", "image/jpeg", "pacientes/documentos/3/placa.jpg", 900_000L, 300_000L));
        when(documentoRepository.save(any(PacienteDocumento.class))).thenAnswer(invocation -> {
            PacienteDocumento doc = invocation.getArgument(0);
            doc.setId(88L);
            return doc;
        });
        MockMultipartFile file = new MockMultipartFile("file", "placa.jpg", "image/jpeg", new byte[]{1, 2, 3});

        var result = service.subir(3L, 50L, file, "  estudio  ");

        assertThat(result.getId()).isEqualTo(88L);
        assertThat(result.getUrl()).isEqualTo("/api/documentos/88/archivo");
        assertThat(result.getCompressedSizeBytes()).isEqualTo(300_000L);
        verify(auditService).registrar(eq("DOCUMENTO_SUBIDO"), anyString(), eq(88L), isNull(), anyString());
    }

    @Test
    void rechazaTurnoQueNoPerteneceAlPaciente() {
        Paciente otro = new Paciente();
        otro.setId(99L);
        Turno turno = new Turno();
        turno.setId(50L);
        turno.setPaciente(otro);
        when(turnoRepository.findById(50L)).thenReturn(Optional.of(turno));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.subir(3L, 50L, file, "estudio"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
        verifyNoInteractions(mediaFileService);
    }


    @Test
    void obtieneDocumentoCuandoElPacienteTienePermiso() {
        PacienteDocumento doc = documento(11L, false);
        when(documentoRepository.findById(11L)).thenReturn(Optional.of(doc));
        when(currentUserService.requireUser())
                .thenReturn(new AuthenticatedUser(1L, 3L, null, RolUsuario.PATIENT, "ana@x.com"));

        assertThat(service.obtenerEntidad(11L)).isSameAs(doc);
    }

    @Test
    void informaDocumentoInexistente() {
        when(documentoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEntidad(404L))
                .isInstanceOf(com.ramirez.mediturnosback.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Documento no encontrado");
    }

    @Test
    void pacientePuedeSubirDocumentoPropioSinTurno() {
        AuthenticatedUser user = new AuthenticatedUser(1L, 3L, null, RolUsuario.PATIENT, "ana@x.com");
        when(currentUserService.requireUser()).thenReturn(user);
        when(mediaFileService.storeMedicalDocument(any(), eq("pacientes/documentos"), eq(3L)))
                .thenReturn(new MediaFileService.StoredFile("dni.pdf", "application/pdf", "pacientes/documentos/3/dni.pdf", 500L, 500L));
        when(documentoRepository.save(any(PacienteDocumento.class))).thenAnswer(invocation -> {
            PacienteDocumento doc = invocation.getArgument(0);
            doc.setId(90L);
            return doc;
        });
        MockMultipartFile file = new MockMultipartFile("file", "dni.pdf", "application/pdf", new byte[]{1});

        var result = service.subir(3L, null, file, "dni");

        assertThat(result.getId()).isEqualTo(90L);
        verify(documentoRepository).save(argThat(doc -> doc.getTurno() == null && "PATIENT".equals(doc.getSubidoPorRol())));
    }

    @Test
    void profesionalRelacionadoPuedeSubirDocumento() {
        AuthenticatedUser user = new AuthenticatedUser(5L, null, 7L, RolUsuario.PROFESSIONAL, "medico@x.com");
        when(currentUserService.requireUser()).thenReturn(user);
        when(turnoRepository.existsByPacienteIdAndProfesionalId(3L, 7L)).thenReturn(true);
        when(mediaFileService.storeMedicalDocument(any(), anyString(), eq(3L)))
                .thenReturn(new MediaFileService.StoredFile("orden.png", "image/png", "ruta.png", 400L, 300L));
        when(documentoRepository.save(any(PacienteDocumento.class))).thenAnswer(invocation -> {
            PacienteDocumento doc = invocation.getArgument(0);
            doc.setId(91L);
            return doc;
        });
        MockMultipartFile file = new MockMultipartFile("file", "orden.png", "image/png", new byte[]{1});

        assertThat(service.subir(3L, null, file, "orden médica").getId()).isEqualTo(91L);
    }

    private PacienteDocumento documento(Long id, boolean archivado) {
        PacienteDocumento doc = new PacienteDocumento();
        doc.setId(id);
        doc.setPaciente(paciente);
        doc.setNombreArchivo("estudio.pdf");
        doc.setMimeType("application/pdf");
        doc.setTipoDocumento("estudio");
        doc.setStoragePath("pacientes/documentos/3/estudio.pdf");
        doc.setOriginalSizeBytes(1000L);
        doc.setStoredSizeBytes(900L);
        doc.setArchivado(archivado);
        doc.setCreadoEn(LocalDateTime.of(2026, 6, 17, 10, 0));
        return doc;
    }
}
