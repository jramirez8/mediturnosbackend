package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitucionDto {
    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String whatsapp;
    private String tipo;
}
