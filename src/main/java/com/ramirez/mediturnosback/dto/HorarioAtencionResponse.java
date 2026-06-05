package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioAtencionResponse {
    private Long id;
    private Long profesionalInstitucionId;
    private Long especialidadId;
    private String especialidad;
    private String diaSemana;
    private LocalTime horaDesde;
    private LocalTime horaHasta;
    private Integer duracionTurnoMin;
    private Boolean activo;
}
