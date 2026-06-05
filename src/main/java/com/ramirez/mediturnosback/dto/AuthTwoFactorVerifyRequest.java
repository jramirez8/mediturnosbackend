package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class AuthTwoFactorVerifyRequest {
    private Long usuarioId;
    private String codigo;
}
