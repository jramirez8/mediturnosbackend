package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TurnoReprogramacionRequest {
    @NotNull
    private Long profesionalId;
    private Long profesionalInstitucionId;
    private Long especialidadId;
    @NotNull
    @Future
    private LocalDateTime fechaHora;
}
