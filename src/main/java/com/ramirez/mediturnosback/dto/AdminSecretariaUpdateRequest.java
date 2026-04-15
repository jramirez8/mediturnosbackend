package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class AdminSecretariaUpdateRequest {
    @Email
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private Long institucionId;
    private Boolean activa;
    private Boolean emailVerificado;
}
