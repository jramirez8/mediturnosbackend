package com.ramirez.mediturnosback.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioAtencionRequest {
    private Long profesionalInstitucionId;
    private Long especialidadId;
    private String diaSemana;
    private LocalTime horaDesde;
    private LocalTime horaHasta;
    private Integer duracionTurnoMin;
    private Boolean activo;
}
