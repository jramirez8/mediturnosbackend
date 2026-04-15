package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminSecretariaRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank
    private String dni;
    private String telefono;
    @NotNull
    private Long institucionId;
    private Boolean activa = true;
    private Boolean emailVerificado = true;
}
