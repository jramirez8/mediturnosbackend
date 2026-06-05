package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class ConsentimientoRequest {
    private Boolean aceptado;
    private String texto;
}
