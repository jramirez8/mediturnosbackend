package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class AdminProfesionalUpdateRequest {
    @Email
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String dni;
    private String matricula;
    private String telefono;
    private Boolean activo;
    private Boolean emailVerificado;
    private List<Long> especialidadIds;
    private List<Long> institucionIds;
}
