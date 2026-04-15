package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminProfesionalRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    private String dni;
    @NotBlank
    private String matricula;
    private String telefono;
    private Boolean activo = true;
    private Boolean emailVerificado = true;
    @NotEmpty
    private List<Long> especialidadIds;
    @NotEmpty
    private List<Long> institucionIds;
}
