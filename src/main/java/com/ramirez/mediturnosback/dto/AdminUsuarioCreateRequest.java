package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUsuarioCreateRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotNull
    private RolUsuario rol;
    private Boolean activo = true;
    private Boolean emailVerificado = true;
}
