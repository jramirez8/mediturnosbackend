package com.ramirez.mediturnosback.dto;

import java.time.LocalDateTime;

public record PacienteDocumentoResponse(
        Long id,
        Long pacienteId,
        Long turnoId,
        String nombreArchivo,
        String mimeType,
        String tipoDocumento,
        Long originalSizeBytes,
        Long storedSizeBytes,
        String url,
        String subidoPorEmail,
        String subidoPorRol,
        Boolean archivado,
        LocalDateTime creadoEn
) {}
