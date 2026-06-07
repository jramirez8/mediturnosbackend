package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.service.TurnoService;
import com.ramirez.mediturnosback.service.MediaFileService;
import com.ramirez.mediturnosback.model.TurnoAdjunto;
import com.ramirez.mediturnosback.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController {

    private final TurnoService turnoService;
    private final JwtService jwtService;
    private final MediaFileService mediaFileService;

    public TurnoController(TurnoService turnoService, JwtService jwtService, MediaFileService mediaFileService) {
        this.turnoService = turnoService;
        this.jwtService = jwtService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping
    public List<TurnoResponse> listarTodos() { return turnoService.listarTodos(); }

    @GetMapping("/{id}")
    public TurnoResponse obtenerPorId(@PathVariable Long id) { return turnoService.obtenerPorId(id); }

    @GetMapping("/paciente/me")
    public List<TurnoResponse> listarTurnosPacienteActual(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long pacienteId = jwtService.extraerPacienteIdDesdeAuthorization(authorization);
        if (pacienteId == null) throw new IllegalArgumentException("El token no corresponde a un paciente");
        return turnoService.listarPorPaciente(pacienteId);
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<TurnoResponse> listarPorPaciente(@PathVariable Long pacienteId) { return turnoService.listarPorPaciente(pacienteId); }

    @GetMapping("/historia-clinica/me")
    public List<TurnoResponse> listarHistoriaClinicaActual(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        return turnoService.listarHistoriaClinica(usuarioId);
    }

    @GetMapping("/historia-clinica/{usuarioId}")
    public List<TurnoResponse> listarHistoriaClinica(@PathVariable Long usuarioId) { return turnoService.listarHistoriaClinica(usuarioId); }

    @GetMapping("/disponibilidad")
    public List<DisponibilidadSlotResponse> listarDisponibilidad(@RequestParam Long profesionalInstitucionId) {
        return turnoService.listarDisponibilidad(profesionalInstitucionId);
    }

    @PostMapping("/solicitar")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse solicitar(@Valid @RequestBody TurnoSolicitudRequest request) { return turnoService.solicitar(request); }


    @PostMapping(value = "/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse adjuntarDocumentacion(@PathVariable Long id,
                                                     @RequestPart("file") MultipartFile file,
                                                     @RequestParam(value = "tipo", required = false) String tipoDocumento,
                                                     @RequestParam(value = "tipoDocumento", required = false) String tipoDocumentoAlt) {
        return turnoService.adjuntarDocumentacion(id, file, tipoDocumento != null ? tipoDocumento : tipoDocumentoAlt);
    }

    @GetMapping("/adjuntos/{adjuntoId}/archivo")
    public ResponseEntity<Resource> descargarAdjunto(@PathVariable Long adjuntoId) {
        TurnoAdjunto adjunto = turnoService.obtenerAdjunto(adjuntoId);
        Resource resource = mediaFileService.loadAsResource(adjunto.getStoragePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(adjunto.getStorageMimeType() != null ? adjunto.getStorageMimeType() : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + adjunto.getNombreArchivo() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}/reprogramar")
    public TurnoResponse reprogramar(@PathVariable Long id, @Valid @RequestBody TurnoReprogramacionRequest request) {
        return turnoService.reprogramar(id, request);
    }

    @PutMapping("/{id}/estado")
    public TurnoResponse actualizarEstado(@PathVariable Long id, @Valid @RequestBody TurnoEstadoUpdateRequest request) {
        return turnoService.actualizarEstado(id, request);
    }

    @PutMapping("/{id}/confirmar-asistencia")
    public TurnoResponse confirmarAsistencia(@PathVariable Long id) {
        return turnoService.confirmarAsistencia(id);
    }

    @PutMapping("/{id}/detalle-consulta")
    public TurnoResponse cargarDetalleConsulta(@PathVariable Long id, @RequestBody DetalleConsultaRequest request) {
        return turnoService.cargarDetalleConsulta(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { turnoService.eliminar(id); }
}
