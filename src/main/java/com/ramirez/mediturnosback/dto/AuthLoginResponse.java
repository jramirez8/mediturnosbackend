package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginResponse {
    private Long usuarioId;
    private Long pacienteId;
    private Long profesionalId;
    private RolUsuario rol;
    private String email;
    private String nombreCompleto;
    private boolean emailVerificado;
    private String mensaje;
    private String token;
    private String accessToken;
    private String jwt;
    private String tokenType;
    private Boolean requiereSegundoFactor;
    private String segundoFactorDestino;
}
