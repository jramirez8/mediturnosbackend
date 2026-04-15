package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.TipoInstitucion;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminInstitucionRequest {
    @NotBlank
    private String nombre;
    private TipoInstitucion tipo = TipoInstitucion.OTRO;
    @NotBlank
    private String direccion;
    private String telefono;
    private String whatsapp;
    private Boolean activa = true;
}
