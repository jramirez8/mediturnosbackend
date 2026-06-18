package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackTurnoResponse {
    private Long id;
    private Long turnoId;
    private Long profesionalId;
    private String profesionalNombre;
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime creadoEn;
}
