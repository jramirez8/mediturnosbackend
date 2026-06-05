package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaEsperaResponse {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalInstitucionId;
    private Long especialidadId;
    private String especialidad;
    private LocalDate fechaPreferidaDesde;
    private LocalDate fechaPreferidaHasta;
    private String observaciones;
    private String estado;
    private LocalDateTime creadoEn;
    private LocalDateTime notificadoEn;
}
