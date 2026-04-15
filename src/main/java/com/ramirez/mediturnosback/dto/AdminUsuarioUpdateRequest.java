package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.RolUsuario;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class AdminUsuarioUpdateRequest {
    @Email
    private String email;
    private String password;
    private RolUsuario rol;
    private Boolean activo;
    private Boolean emailVerificado;
}
