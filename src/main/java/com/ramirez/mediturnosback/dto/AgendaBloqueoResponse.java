package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueoResponse {
    private Long id;
    private Long profesionalInstitucionId;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private String motivo;
}
