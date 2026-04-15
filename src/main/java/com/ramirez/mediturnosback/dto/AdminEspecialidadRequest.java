package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminEspecialidadRequest {
    @NotBlank
    private String nombre;
    private Boolean activa = true;
}
