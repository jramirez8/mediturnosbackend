package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminSecretariaResponse {
    private Long id;
    private Long usuarioId;
    private String email;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private Boolean activa;
    private String institucion;
}
