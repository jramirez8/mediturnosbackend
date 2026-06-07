package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.FileUploadResponse;
import com.ramirez.mediturnosback.dto.PacienteDocumentoResponse;
import com.ramirez.mediturnosback.model.PacienteDocumento;
import com.ramirez.mediturnosback.service.DocumentoService;
import com.ramirez.mediturnosback.service.MediaFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final MediaFileService mediaFileService;

    public DocumentoController(DocumentoService documentoService, MediaFileService mediaFileService) {
        this.documentoService = documentoService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping("/me")
    public List<PacienteDocumentoResponse> listarPropios() {
        return documentoService.listarPropios();
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<PacienteDocumentoResponse> listarPorPaciente(@PathVariable Long pacienteId,
                                                              @RequestParam(value = "incluirArchivados", defaultValue = "false") boolean incluirArchivados) {
        return documentoService.listarPorPaciente(pacienteId, incluirArchivados);
    }

    @PostMapping(value = "/paciente/{pacienteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse subir(@PathVariable Long pacienteId,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "tipo", required = false) String tipo,
                                    @RequestParam(value = "tipoDocumento", required = false) String tipoDocumento,
                                    @RequestParam(value = "turnoId", required = false) Long turnoId) {
        return documentoService.subir(pacienteId, turnoId, file, tipo != null ? tipo : tipoDocumento);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        PacienteDocumento doc = documentoService.obtenerEntidad(id);
        Resource resource = mediaFileService.loadAsResource(doc.getStoragePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombreArchivo() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}/archivar")
    public PacienteDocumentoResponse archivar(@PathVariable Long id) {
        return documentoService.archivar(id);
    }
}
