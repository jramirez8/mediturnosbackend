package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminObraSocialRequest {
    @NotBlank
    private String nombre;
    private String codigo;
    private Boolean activa = true;
}
