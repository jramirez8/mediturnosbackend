package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class FeedbackTurnoRequest {
    private Integer puntuacion;
    private String comentario;
}
