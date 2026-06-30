package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.FileUploadResponse;
import com.ramirez.mediturnosback.dto.PacienteDocumentoResponse;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.PacienteDocumentoRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.util.AppClock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
public class DocumentoService {

    private final PacienteDocumentoRepository documentoRepository;
    private final PacienteRepository pacienteRepository;
    private final TurnoRepository turnoRepository;
    private final MediaFileService mediaFileService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final AppClock appClock;

    public DocumentoService(PacienteDocumentoRepository documentoRepository,
                            PacienteRepository pacienteRepository,
                            TurnoRepository turnoRepository,
                            MediaFileService mediaFileService,
                            CurrentUserService currentUserService,
                            AuditService auditService,
                            AppClock appClock) {
        this.documentoRepository = documentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.turnoRepository = turnoRepository;
        this.mediaFileService = mediaFileService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.appClock = appClock;
    }

    @Transactional(readOnly = true)
    public List<PacienteDocumentoResponse> listarPropios() {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PATIENT);
        return listarPorPaciente(user.pacienteId(), false);
    }

    @Transactional(readOnly = true)
    public List<PacienteDocumentoResponse> listarPorPaciente(Long pacienteId, boolean incluirArchivados) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));
        validarLectura(paciente);
        List<PacienteDocumento> docs = incluirArchivados
                ? documentoRepository.findByPacienteIdOrderByCreadoEnDesc(pacienteId)
                : documentoRepository.findByPacienteIdAndArchivadoFalseOrderByCreadoEnDesc(pacienteId);
        return docs.stream().map(this::map).toList();
    }

    @Transactional
    public FileUploadResponse subir(Long pacienteId, Long turnoId, MultipartFile file, String tipoDocumento) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + pacienteId));
        Turno turno = null;
        if (turnoId != null) {
            turno = turnoRepository.findById(turnoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id: " + turnoId));
            if (turno.getPaciente() == null || !Objects.equals(turno.getPaciente().getId(), paciente.getId())) {
                throw new IllegalArgumentException("El turno no pertenece al paciente seleccionado");
            }
        }
        validarEscritura(paciente);
        MediaFileService.StoredFile stored = mediaFileService.storeMedicalDocument(file, "pacientes/documentos", pacienteId);
        AuthenticatedUser user = currentUserService.requireUser();

        PacienteDocumento doc = new PacienteDocumento();
        doc.setPaciente(paciente);
        doc.setTurno(turno);
        doc.setNombreArchivo(stored.originalName());
        doc.setMimeType(stored.mimeType());
        doc.setTipoDocumento(normalizar(tipoDocumento));
        doc.setStoragePath(stored.relativePath());
        doc.setOriginalSizeBytes(stored.originalSizeBytes());
        doc.setStoredSizeBytes(stored.storedSizeBytes());
        doc.setSubidoPorUsuarioId(user.usuarioId());
        doc.setSubidoPorEmail(user.email());
        doc.setSubidoPorRol(user.rol() != null ? user.rol().name() : null);
        doc.setArchivado(false);
        PacienteDocumento guardado = documentoRepository.save(doc);
        auditService.registrar("DOCUMENTO_SUBIDO", "paciente_documentos", guardado.getId(), null, "Documento subido para paciente " + pacienteId);

        return new FileUploadResponse(
                guardado.getId(),
                guardado.getNombreArchivo(),
                guardado.getMimeType(),
                guardado.getOriginalSizeBytes(),
                guardado.getStoredSizeBytes(),
                url(guardado),
                "Documento subido correctamente"
        );
    }

    @Transactional(readOnly = true)
    public PacienteDocumento obtenerEntidad(Long id) {
        PacienteDocumento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
        validarLectura(doc.getPaciente());
        return doc;
    }

    @Transactional
    public PacienteDocumentoResponse archivar(Long id) {
        PacienteDocumento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id: " + id));
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY, RolUsuario.PROFESSIONAL);
        if (user.isProfessional() && !tieneRelacionConPaciente(user, doc.getPaciente())) {
            throw new AccessDeniedException("No podés archivar documentos de este paciente");
        }
        doc.setArchivado(true);
        doc.setArchivadoEn(appClock.now());
        PacienteDocumento guardado = documentoRepository.save(doc);
        auditService.registrar("DOCUMENTO_ARCHIVADO", "paciente_documentos", id, null, "Documento archivado");
        return map(guardado);
    }

    private void validarLectura(Paciente paciente) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && Objects.equals(user.pacienteId(), paciente.getId())) return;
        if (user.isProfessional() && tieneRelacionConPaciente(user, paciente)) return;
        throw new AccessDeniedException("No podés acceder a documentos de este paciente");
    }

    private void validarEscritura(Paciente paciente) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && Objects.equals(user.pacienteId(), paciente.getId())) return;
        if (user.isProfessional() && tieneRelacionConPaciente(user, paciente)) return;
        throw new AccessDeniedException("No podés subir documentos para este paciente");
    }

    private boolean tieneRelacionConPaciente(AuthenticatedUser user, Paciente paciente) {
        return user.profesionalId() != null
                && paciente != null
                && turnoRepository.existsByPacienteIdAndProfesionalId(paciente.getId(), user.profesionalId());
    }

    private PacienteDocumentoResponse map(PacienteDocumento doc) {
        return new PacienteDocumentoResponse(
                doc.getId(),
                doc.getPaciente() != null ? doc.getPaciente().getId() : null,
                doc.getTurno() != null ? doc.getTurno().getId() : null,
                doc.getNombreArchivo(),
                doc.getMimeType(),
                doc.getTipoDocumento(),
                doc.getOriginalSizeBytes(),
                doc.getStoredSizeBytes(),
                url(doc),
                doc.getSubidoPorEmail(),
                doc.getSubidoPorRol(),
                doc.getArchivado(),
                doc.getCreadoEn()
        );
    }

    private String url(PacienteDocumento doc) {
        if (doc == null || doc.getId() == null) return null;
        return "/api/documentos/" + doc.getId() + "/archivo";
    }

    private String normalizar(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
