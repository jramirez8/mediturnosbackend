package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.EstadoTurno;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TurnoEstadoUpdateRequest {
    @NotNull
    private EstadoTurno estado;
}
