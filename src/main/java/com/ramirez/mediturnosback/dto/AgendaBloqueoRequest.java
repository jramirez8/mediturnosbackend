package com.ramirez.mediturnosback.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgendaBloqueoRequest {
    private Long profesionalInstitucionId;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private String motivo;
}
