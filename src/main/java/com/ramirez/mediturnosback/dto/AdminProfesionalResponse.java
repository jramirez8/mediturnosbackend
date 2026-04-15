package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AdminProfesionalResponse {
    private Long id;
    private Long usuarioId;
    private String email;
    private String nombre;
    private String apellido;
    private String dni;
    private String matricula;
    private String telefono;
    private Boolean activo;
    private List<String> especialidades;
    private List<String> instituciones;
}
