package com.ramirez.mediturnosback.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ListaEsperaRequest {
    private Long pacienteId;
    private Long profesionalInstitucionId;
    private Long especialidadId;
    private LocalDate fechaPreferidaDesde;
    private LocalDate fechaPreferidaHasta;
    private String observaciones;
}
