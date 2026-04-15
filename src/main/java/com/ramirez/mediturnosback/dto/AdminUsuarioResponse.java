package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUsuarioResponse {
    private Long id;
    private String email;
    private RolUsuario rol;
    private Boolean activo;
    private Boolean emailVerificado;
    private Long pacienteId;
    private Long profesionalId;
    private Long secretariaId;
    private String nombreMostrar;
    private String dni;
}
