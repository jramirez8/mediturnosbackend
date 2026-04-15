package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TurnoSolicitudRequest {
    @NotNull
    private Long pacienteId;
    @NotNull
    private Long profesionalId;
    private Long profesionalInstitucionId;
    private Long especialidadId;
    @NotNull
    @Future
    private LocalDateTime fechaHora;
    private String observaciones;
    private String documentacionNombreArchivo;
    private String documentacionMimeType;
    private String documentacionBase64;
}
