package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.EstadoTurno;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoRequest {

    @NotNull
    private LocalDateTime fechaHora;

    @NotNull
    private EstadoTurno estado;

    private String observaciones;

    @NotNull
    private Long pacienteId;

    @NotNull
    private Long profesionalId;
}