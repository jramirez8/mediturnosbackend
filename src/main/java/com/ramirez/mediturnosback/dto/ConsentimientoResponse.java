package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentimientoResponse {
    private Long pacienteId;
    private Boolean aceptado;
    private LocalDateTime aceptadoEn;
    private String texto;
}
