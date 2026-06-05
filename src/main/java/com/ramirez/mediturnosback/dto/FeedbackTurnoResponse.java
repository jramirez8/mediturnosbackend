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
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime creadoEn;
}
